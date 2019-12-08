package com.ccc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ccc.contract.OrderContract
import com.ccc.contract.OrderContract2
import com.ccc.contract.StockContract
import com.ccc.state.*
import net.corda.core.contracts.Command
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class OrderCancelFlow2(private val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>()  {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        //Move order to cancelled status
        val orderStatesPage = serviceHub.vaultService.queryBy(Order2::class.java)
        val orderStateAndRef = requireNotNull(orderStatesPage.states.find { it.state.data.linearId == linearId })
        val order = orderStateAndRef.state.data
        val cancelledOrder = order.copy(orderStatus = OrderStatus2.CANCELLED)

        //Get the Stock and its StateAndRef
        val stockStatesPages = serviceHub.vaultService.queryBy(Stock::class.java)
        val stockStateAndRef =
            requireNotNull(stockStatesPages.states.find { it.state.data.code == cancelledOrder.ticker })
        val inputStock = stockStateAndRef.state.data
        val outputStock = inputStock.deList()


        //Create a Command to delist the Stock
        val stockDelistCommand = Command(StockContract.Commands.Delist(), outputStock.participants.map { it.owningKey })


        //Create a Command to end the Order
        val orderCancelCommand = Command(OrderContract2.Commands.Cancel(), cancelledOrder.participants.map { it.owningKey })


        //Create a txBuilder
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val txBuilder = TransactionBuilder(notary)
        //Place all the above contents in the txBuilder
        txBuilder.addInputState(stockStateAndRef)
            .addInputState(orderStateAndRef)
            .addCommand(stockDelistCommand)
            .addCommand(orderCancelCommand)
            .addOutputState(outputStock)
            .addOutputState(cancelledOrder)
            .setTimeWindow(TimeWindow.fromOnly(serviceHub.clock.instant()))
        //Verify the builder with the serviceHub before signing the Tx (This will run the contracts)
        txBuilder.verify(serviceHub)
        //Self sign initialTx of the txBuilder
        val signInitialTx = serviceHub.signInitialTransaction(txBuilder)
        //FinalityFlow and also broadcast to everyone in the network that this Sell Order has end
        val signedTransaction = subFlow(FinalityFlow(signInitialTx, emptyList()))
        val broadcastList = serviceHub.networkMapCache.allNodes
            .map { node -> node.legalIdentities.first() } - cancelledOrder.seller - notary
        subFlow(BroadcastTransactionFlow(signedTransaction, broadcastList))
        return signedTransaction
    }


    @InitiatedBy(OrderCancelFlow2::class)
    class OrderCancelFlowResponder(val otherSideSession: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            val receiveTransactionFlow = object : ReceiveTransactionFlow(otherSideSession, statesToRecord = StatesToRecord.ALL_VISIBLE) {
                override fun checkBeforeRecording(stx: SignedTransaction) {/* add tx checks */}
            }
            subFlow(receiveTransactionFlow)
        }

    }

}
