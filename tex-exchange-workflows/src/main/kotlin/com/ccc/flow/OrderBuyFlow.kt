package com.ccc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ccc.contract.OrderContract
import com.ccc.state.Order
import com.google.common.annotations.VisibleForTesting
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.node.services.VaultService
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.contextLogger

/**
 * This flow is to Buy the Stock
 * The Buyer initiates the flow
 * @param orderId The Unique ID of the Order
 */
@InitiatingFlow
@StartableByRPC
class OrderBuyFlow(private val stockId: UniqueIdentifier, private val stockQuantity: Long) : FlowLogic<List<Order>>() {
    override val progressTracker = ProgressTracker()

    private companion object {
        private val log = contextLogger()
    }

    @Suspendable
    override fun call(): List<Order> {
        val sortedOrders = getSortedOrdersForStock(serviceHub.vaultService, stockId)

        // Iterate through sortedOrderStates and buy as much stock as we need.
        // Stock could be from different counter parties so 1 transaction per stock buying.
        var i = 0
        var stockQuantityNeeded = stockQuantity
        val byMe = ourIdentity
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        val boughtOrders = mutableListOf<Order>()
        while (stockQuantityNeeded > 0) {
            // Buy a stock.
            val sellOrder =  sortedOrders[i]
            val buyOrder = sellOrder.state.data.buy(byMe)
            val txBuilder = TransactionBuilder(notary = notary)
                .addInputState(sellOrder)
                .addOutputState(buyOrder)
                .addCommand(OrderContract.Commands.Buy(), listOf(ourIdentity.owningKey, buyOrder.seller.owningKey))
            txBuilder.verify(serviceHub)
            val stx = serviceHub.signInitialTransaction(txBuilder)
            val seller = buyOrder.seller
            val flowSession = initiateFlow(seller)
            val ftx = subFlow(CollectSignaturesFlow(stx, setOf(flowSession)))
            subFlow(FinalityFlow(ftx, setOf(flowSession)))
            stockQuantityNeeded -= buyOrder.stockQuantity
            boughtOrders.add(buyOrder)
            i++
            if (i >= sortedOrders.size) {
                log.info("No more orders to buy for stock $stockId")
                break
            }
        }
        return boughtOrders
    }

    @VisibleForTesting
    fun getSortedOrdersForStock(vaultService: VaultService, stockId: UniqueIdentifier)  = vaultService
        .queryBy(Order::class.java).states
        .filter { it.state.data.stockId == stockId }
        .sortedBy { it.state.data.pricePerUnit() }

    private fun Order.pricePerUnit(): Long {
        return price.splitEvenly(stockQuantity.toInt())[0].quantity
    }
}

// The original seller will run this code. He should transfer the stock and broadcast the the Order.BOUGHT to everyone else.
@InitiatedBy(OrderBuyFlow::class)
class OrderBuyFlowResponder(val flowSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                // TODO: Add additional checks here
            }
        }
        val txId = subFlow(signedTransactionFlow).id
        subFlow(ReceiveFinalityFlow(flowSession, expectedTxId = txId))
    }
}