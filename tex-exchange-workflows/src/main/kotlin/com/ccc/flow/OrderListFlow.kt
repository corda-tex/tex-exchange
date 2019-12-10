package com.ccc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ccc.contract.OrderContract
import com.ccc.contract.StockContract
import com.ccc.state.Order
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.lang.IllegalStateException
import java.time.Instant
import java.util.*

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

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val stockStateAndRef = subFlow(UtilsFlows.GetStockFlow(stockId, stockQuantity)) ?: throw StockNotFoundException(stockId)
        // Create a Sell Order.
        val sellOrder =  Order(stockId = stockId,
                                stockDescription = stockStateAndRef.state.data.description,
                                stockQuantity = stockQuantity,
                                price = stockPrice,
                                state = Order.State.SELL,
                                expiryDateTime = expiry,
                                seller = ourIdentity,
                                buyer = null)
        val outputStock = stockStateAndRef.state.data.list() // create a stock state and list it.
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val txBuilder = TransactionBuilder(notary)
            .withItems(
                stockStateAndRef,
                Command(StockContract.Commands.List(), ourIdentity.owningKey),
                StateAndContract(outputStock, StockContract.STOCK_CONTRACT_REF),
                Command(OrderContract.Commands.List(), ourIdentity.owningKey),
                StateAndContract(sellOrder, OrderContract.ORDER_CONTRACT_REF),
                TimeWindow.between(serviceHub.clock.instant(), sellOrder.expiryDateTime)
            )
        txBuilder.verify(serviceHub)
        val signedInitialTx = serviceHub.signInitialTransaction(txBuilder)
        val counterPartiesSessions = serviceHub.networkMapCache.allNodes
            .asSequence()
            .filter { it.legalIdentities.first() != sellOrder.seller && it.legalIdentities.first() != notary }
            .map { it.legalIdentities.first() }.map { initiateFlow(it) }.toSet()
        return subFlow(FinalityFlow(signedInitialTx, counterPartiesSessions))
        //TODO: Take the output state of the Sell Order and figure out the UUID of the sell order
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