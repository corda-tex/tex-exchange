package com.ccc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ccc.contract.OrderContract2
import com.ccc.state.Direction
import com.ccc.state.Order2
import com.ccc.types.StockTokenType
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.GBP
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap
import java.math.BigDecimal
import java.time.Instant

object OrderFlow {

    @InitiatingFlow
    @StartableByRPC
    class OrderListFlow2(
        private val ticker: String,
        private val units: Int,
        private val price: BigDecimal,
        private val expires: Instant
    ) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {

            val stock = serviceHub.vaultService.queryBy(FungibleToken::class.java)
            val stockStateAndRef = stock.states.find { it.state.data.tokenType.tokenIdentifier == ticker }
                ?: throw IllegalArgumentException("Stock with '$ticker' not found in vault.")

            val quantityOfStockInVault =
                stock.states.filter { it.state.data.tokenType.tokenIdentifier == ticker }
                    .sumBy { it.state.data.amount.quantity.toInt() }
            val sellOrders = serviceHub.vaultService.queryBy(Order2::class.java)
            val quantityOfStockInSellOrders = sellOrders.states.sumBy { it.state.data.units }
            val quantityOfAvailableStock = quantityOfStockInVault - quantityOfStockInSellOrders
            if (units > quantityOfAvailableStock) {
                throw IllegalArgumentException("Insufficient stock in vault. Found $quantityOfStockInVault units of $ticker.")
            }

            // create a sell order
            val inputOrder = Order2(
                linearId = UniqueIdentifier(),
                ticker = ticker,
                units = units,
                price = price,
                direction = Direction.SELL,
                expires = expires,
                seller = ourIdentity
//                buyer = null
            )

            // create a command to list the sell order
            val commandOrderList = Command(OrderContract2.Commands.List(), ourIdentity.owningKey)

            // create a transaction builder with the following items
            val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities[0])
                .withItems(
                    commandOrderList,
                    StateAndContract(inputOrder, OrderContract2.ORDER_CONTRACT_2_REF),
                    TimeWindow.between(serviceHub.clock.instant(), inputOrder.expires)
                )

            // We check our transaction is valid based on its contracts.
            txBuilder.verify(serviceHub)

            // We sign the transaction with our private key, making it immutable.
            val signedInitialTx = serviceHub.signInitialTransaction(txBuilder)
            val notary = serviceHub.networkMapCache.notaryIdentities.first()

            //Create a FinalityFlow and also BroadcastTx to all the parties
            val counterPartySessions = serviceHub.networkMapCache.allNodes
                .filter { it.legalIdentities.first() != inputOrder.seller && it.legalIdentities.first() != notary }
                .map { it.legalIdentities.first() }.map { initiateFlow(it) }.toSet()
            return subFlow(FinalityFlow(signedInitialTx, counterPartySessions))
        }
    }

    @InitiatedBy(OrderListFlow2::class)
    class OrderListFlowResponder2(val otherSideSession: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            subFlow(ReceiveFinalityFlow(otherSideSession, statesToRecord = StatesToRecord.ALL_VISIBLE))
        }
    }

    @InitiatingFlow
    @StartableByRPC
    class OrderBuyFlow2(
//        private val ticker: String,
        private val linearId: String
//        private val units: Int
    ) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {

            // simplified order buy -> buy all stocks in a given order linearId

            val sellOrder = serviceHub.vaultService.queryBy(Order2::class.java).states.firstOrNull { it.state.data.linearId == UniqueIdentifier.fromString(linearId) }
                ?: throw IllegalArgumentException("Couldn't find order state with $linearId")

            if (sellOrder.state.data.units == 0) {
                throw IllegalArgumentException("0 units of ${sellOrder.state.data.ticker} available for sale")
            }

            val gbpInVault = BigDecimal(
                serviceHub.vaultService.queryBy(FungibleToken::class.java).states.filter {
                    it.state.data.holder == ourIdentity &&
                            it.state.data.tokenType.tokenIdentifier == "GBP"
                }.sumByDouble { it.state.data.amount.quantity.toDouble() }
            ).movePointLeft(2)

            if (gbpInVault.compareTo(sellOrder.state.data.price.setScale(2).times(BigDecimal(sellOrder.state.data.units))) < 0) {
                throw IllegalArgumentException("Order price: ${sellOrder.state.data.price.setScale(2).times(BigDecimal(sellOrder.state.data.units))}, available in vault: $gbpInVault")
            }

            val txBuilder = TransactionBuilder(serviceHub.networkMapCache.notaryIdentities[0])

            txBuilder.addInputState(sellOrder)

            txBuilder.addOutputState(sellOrder.state.data.copy(
                units = 0,
                buyers = sellOrder.state.data.buyers.plus(ourIdentity)
            ))

            txBuilder.addCommand(Command(OrderContract2.Commands.Buy(),
                sellOrder.state.data.participants.map { it.owningKey } + ourIdentity.owningKey))
            txBuilder.verify(serviceHub)

//            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            val everyoneElse = sellOrder.state.data.participants
            val signedInitialTx = serviceHub.signInitialTransaction(txBuilder)

            //Create a FinalityFlow and also BroadcastTx to all the parties
            val counterPartySessions = everyoneElse.map { initiateFlow(it as Party) }
            //Send GBP to seller
            subFlow(MoveFungibleTokens(sellOrder.state.data.price.times(BigDecimal(sellOrder.state.data.units)) of GBP, sellOrder.state.data.seller))
            //Send sellOrder state to counter party seller -> from this the seller can derive the units
            counterPartySessions.forEach { it.send(sellOrder.state.data) }
            val signedByAllTx = subFlow(CollectSignaturesFlow(signedInitialTx, counterPartySessions))
            return subFlow(FinalityFlow(signedByAllTx, counterPartySessions))
        }
    }

    @InitiatedBy(OrderBuyFlow2::class)
    class OrderBuyFlowResponder2(val flowSession: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    // TODO: Add additional checks here
                }
            }

            //Receive sell order state data from buyer
            val sellOrder = flowSession.receive<Order2>().unwrap { it }

            //Send stock tokens to seller
            subFlow(MoveFungibleTokens(sellOrder.units of StockTokenType(sellOrder.ticker), flowSession.counterparty))

            subFlow(signedTransactionFlow)

            subFlow(ReceiveFinalityFlow(flowSession, statesToRecord = StatesToRecord.ALL_VISIBLE))
        }
    }
}
