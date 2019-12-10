package com.ccc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ccc.contract.OrderContract
import com.ccc.contract.StockContract
import com.ccc.state.Order
import com.ccc.state.Stock
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.lang.IllegalStateException
import java.time.Instant
import java.util.*

// 1. Put in Stock Order's id.
// 2. Remove Stock from being published in OrderListFlow


/**
 * Prerequisite: SelfIssueStockFlow and get a UniqueIdentifier of the Stock
 *
 * This flow would create a Sell Order for a Stock and broadcast the data to all the nodes in the network.
 * The flow would return the order ID. Use this id in other flows.
 */
@InitiatingFlow
@StartableByRPC
class OrderListFlow(
    private val stockId: UniqueIdentifier,
    private val stockPrice: Amount<Currency>,
    private val stockQuantity: Long,
    private val expiry: Instant
) : FlowLogic<SignedTransaction>() {

    //override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val stockForOrderIn = subFlow(UtilsFlows.GetStockFlow(stockId, stockQuantity)) ?: throw StockNotFoundException(stockId)
        // Create a Sell Order.
        val sellOrder =  Order( stockId = stockId,
                                stockDescription = stockForOrderIn.state.data.description,
                                stockQuantity = stockQuantity,
                                price = stockPrice,
                                state = Order.State.SELL,
                                expiryDateTime = expiry,
                                seller = ourIdentity,
                                buyer = null)

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        // 1 Transaction to register listed stock tagged with orderId in our vault.
        reserveStockForOrder(notary, stockForOrderIn, sellOrder.linearId)
        // 1 Transaction to broadcast the order to all counterparties.
        val txBuilder = TransactionBuilder(notary)
            .addOutputState(sellOrder)
            .addCommand(OrderContract.Commands.List(), ourIdentity.owningKey)
            .setTimeWindow(TimeWindow.between(serviceHub.clock.instant(), sellOrder.expiryDateTime))
        txBuilder.verify(serviceHub)
        val stx = serviceHub.signInitialTransaction(txBuilder)
        val counterPartiesSessions = serviceHub.networkMapCache.allNodes
            .asSequence()
            .filter { it.legalIdentities.first() != sellOrder.seller && it.legalIdentities.first() != notary }
            .map { it.legalIdentities.first() }.map { initiateFlow(it) }.toSet()
        return subFlow(FinalityFlow(stx, counterPartiesSessions))
        //TODO: Take the output state of the Sell Order and figure out the UUID of the sell order
    }

    @Suspendable
    private fun reserveStockForOrder(notary: Party, stockForOrderIn: StateAndRef<Stock>, sellOrderId: UniqueIdentifier) {
        val stockForOrderOut = stockForOrderIn.state.data.list(sellOrderId) // create a stock state and list it.
        val txBuilder = TransactionBuilder(notary)
            .addInputState(stockForOrderIn)
            .addOutputState(stockForOrderOut)
            .addCommand(StockContract.Commands.Reserve(), ourIdentity.owningKey)
        txBuilder.verify(serviceHub)
        val stx = serviceHub.signInitialTransaction(txBuilder)
        subFlow(FinalityFlow(stx, emptySet<FlowSession>()))
    }
}

@InitiatedBy(OrderListFlow::class)
class OrderListFlowResponder(private val otherSideSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(otherSideSession, statesToRecord = StatesToRecord.ALL_VISIBLE))
    }

}

class StockNotFoundException(stockId: UniqueIdentifier) : IllegalStateException("Stock with '$stockId' does not exist")