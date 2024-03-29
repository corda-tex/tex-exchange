package com.ccc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ccc.contract.OrderContract
import com.ccc.contract.StockContract
import com.ccc.state.Order
import com.ccc.state.Stock
import net.corda.core.contracts.Command
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

/**
 * Flow to end the Sell Order and Delist the Stock
 * Only the Seller can end the Sell Order
 *
**/

@InitiatingFlow
@StartableByRPC
class OrderEndFlow(val orderID: UniqueIdentifier) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        //Get the Order and its StateAndRef
        val orderStatesPage = serviceHub.vaultService.queryBy(Order::class.java)
        val orderStateAndRef = requireNotNull(orderStatesPage.states.find { it.state.data.linearId == orderID })
        val order = orderStateAndRef.state.data
        //Get the Stock and its StateAndRef
        val stockStatesPages = serviceHub.vaultService.queryBy(Stock::class.java)
        val stockStateAndRef =
            requireNotNull(stockStatesPages.states.find { it.state.data.linearId == order.stockLinearId })
        val inputStock = stockStateAndRef.state.data
        val outputStock = inputStock.deList()
        //Create a Command to delist the Stock
        val stockDelistCommand = Command(StockContract.Commands.Delist(), outputStock.participants.map { it.owningKey })
        //Create a Command to end the Order
        val orderEndCommand = Command(OrderContract.Commands.End(), order.participants.map { it.owningKey })
        //Create a txBuilder
        val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        //Place all the above contents in the txBuilder
        txBuilder.addInputState(stockStateAndRef)
                 .addInputState(orderStateAndRef)
                 .addCommand(stockDelistCommand)
                 .addCommand(orderEndCommand)
                 .addOutputState(outputStock)
                 .setTimeWindow(TimeWindow.fromOnly(serviceHub.clock.instant()))
        //Self sign initialTx of the txBuilder
        val signInitialTx = serviceHub.signInitialTransaction(txBuilder)
        //Create a list of everyOneElse except self
        val everyoneElse = order.participants - ourIdentity
        //Initiate Flow and get the flow sessions from everyOneElse
        val flowSessionsOfEveryoneElse = everyoneElse.map { initiateFlow(it) }
        //Use the flowSessions of everyOneElse and CollectSignatures
        val signAllTx = subFlow(CollectSignaturesFlow(signInitialTx, flowSessionsOfEveryoneElse))
        //FinalityFlow and also broadcast to everyone in the network that this Sell Order has end
        return subFlow(FinalityFlow(signAllTx, flowSessionsOfEveryoneElse)).also {
            val broadcastParties = serviceHub.networkMapCache.allNodes.map { nodeInfo -> nodeInfo.legalIdentities.first() } - order.participants
            subFlow(BroadcastTransactionFlow(it, broadcastParties))
        }
    }

}

@InitiatedBy(OrderEndFlow::class)
class OrderEndFlowResponder(val flowSession: FlowSession) : FlowLogic<Unit>() {
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
