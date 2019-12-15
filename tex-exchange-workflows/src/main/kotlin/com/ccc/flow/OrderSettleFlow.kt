package com.ccc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ccc.contract.OrderContract
import com.ccc.state.Order
import com.ccc.state.Stock
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

/**
 * Take the Order
 * Transfer the Stock to the Buyer
 * Broadcast to everyone that this event has happened
 *
**/

@InitiatingFlow
@StartableByRPC
class OrderSettleFlow : FlowLogic<List<Stock>>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): List<Stock> {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        // Find in the vault for Bought Orders
        val boughtOrders = serviceHub.vaultService.queryBy(Order::class.java)
            .states.filter { it.state.data.state == Order.State.BOUGHT }

        val settledStocks = mutableListOf<Stock>()

        for (boughtOrder in boughtOrders) {
            val newOwner = boughtOrder.state.data.buyer
            val orderId = boughtOrder.state.data.linearId
            val stockIn = serviceHub.vaultService.queryBy(Stock::class.java).states.filter { it.state.data.orderId == orderId }.single()
            val stockOut = stockIn.state.data.withNewOwner(newOwner!!)
            val settledOrder =  boughtOrder.state.data.settle()
            val txBuilder = TransactionBuilder(notary = notary)
                .addInputState(stockIn)
                .addOutputState(stockOut.ownableState)
                .addCommand(stockOut.command, listOf(ourIdentity.owningKey, newOwner.owningKey))
                .addOutputState(settledOrder)
                .addCommand(OrderContract.Commands.Settle(), listOf(ourIdentity.owningKey, newOwner.owningKey))
            txBuilder.verify(serviceHub)
            val stx = serviceHub.signInitialTransaction(txBuilder)
            val buyer = boughtOrder.state.data.participants[1]
            val buyerSession = initiateFlow(buyer)
            val ftx = subFlow(CollectSignaturesFlow(stx, setOf(buyerSession)))
            subFlow(FinalityFlow(ftx, setOf(buyerSession)))
            settledStocks.add((stockOut.ownableState as Stock))
        }

        return settledStocks
    }
}

@InitiatedBy(OrderSettleFlow::class)
class OrderSettleFlowResponder(val flowSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) {
                //TODO("not implemented")
            }
        }
        subFlow(signedTransactionFlow)
        subFlow(ReceiveFinalityFlow(flowSession, statesToRecord = StatesToRecord.ALL_VISIBLE))
    }
}