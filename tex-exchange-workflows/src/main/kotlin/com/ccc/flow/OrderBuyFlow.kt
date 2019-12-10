//package com.ccc.flow
//
//import co.paralleluniverse.fibers.Suspendable
//import com.ccc.contract.OrderContract
//import com.ccc.contract.OrderContract.Companion.ORDER_CONTRACT_REF
//import com.ccc.state.Order
//import net.corda.core.contracts.*
//import net.corda.core.transactions.SignedTransaction
//import net.corda.core.transactions.TransactionBuilder
//import net.corda.core.utilities.ProgressTracker
//import net.corda.core.flows.*
//import net.corda.core.node.StatesToRecord
//import java.util.*
//
//// TODO: kyriakos: as is, this flow should be renamed to ReserveStockFlow(?)
//
///**
// * This flow is to Buy the Stock
// * The Buyer initiates the flow
// * @param orderId The Unique ID of the Order
// */
//@InitiatingFlow
//@StartableByRPC
//class OrderBuyFlow(val stockId: UniqueIdentifier, val stockQuantity: Long) : FlowLogic<SignedTransaction>() {
//    override val progressTracker = ProgressTracker()
//
//    @Suspendable
//    override fun call(): SignedTransaction {
//        val orderStates = serviceHub.vaultService.queryBy(Order::class.java).states
//            .filter { it.state.data.stockId == stockId }.sortedBy { it.state.data.pricePerUnit() }
//
//        val orderStateAndRef = requireNotNull(orderStates.states.find { it.state.data.linearId == orderId })
//        val inputOrder = orderStateAndRef.state.data
//        //Create an OrderOutput with current node's id as  a Buyer
//        val outputOrder = inputOrder.buy(amount, ourIdentity) //TODO: kyriakos: why copy the price, the price is already there.
//        val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.get(0))
//            .withItems(orderStateAndRef,
//                StateAndContract(outputOrder, ORDER_CONTRACT_REF),
//                Command(OrderContract.Commands.Buy(), outputOrder.participants.map { it.owningKey } + ourIdentity.owningKey),
//                TimeWindow.between(serviceHub.clock.instant(), outputOrder.expiryDateTime)
//                )
//        txBuilder.verify(serviceHub)
//        val signedInitialTx = serviceHub.signInitialTransaction(txBuilder)
//        val everyOneElse = outputOrder.participants - ourIdentity
//        // initiate the flow with the above participants and get the Flow sessions
//        val flowSessions = everyOneElse.map { initiateFlow(it) }
//        // CollectSignaturesFlow with the people
//        val signedByAllTx = subFlow(CollectSignaturesFlow(signedInitialTx, flowSessions))
//        //return signedByAllTx
//        // Finality Flow with the above mentioned people
//        return subFlow(FinalityFlow(signedByAllTx, flowSessions))
//    }
//
//    private fun Order.pricePerUnit(): Int {
//        return price.splitEvenly(stockQuantity.toInt()).size
//    }
//}
//
//@InitiatedBy(OrderBuyFlow::class)
//class OrderBuyFlowResponder(val flowSession: FlowSession) : FlowLogic<Unit>() {
//    @Suspendable
//    override fun call() {
//        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
//            override fun checkTransaction(stx: SignedTransaction) = requireThat {
//                // TODO: Add additional checks here
//            }
//        }
//        subFlow(signedTransactionFlow)
//        subFlow(ReceiveFinalityFlow(flowSession, statesToRecord = StatesToRecord.ALL_VISIBLE))
//    }
//}