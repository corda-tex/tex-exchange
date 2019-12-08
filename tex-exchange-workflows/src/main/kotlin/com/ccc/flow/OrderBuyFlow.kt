package com.ccc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ccc.contract.OrderContract
import com.ccc.contract.OrderContract.Companion.ORDER_CONTRACT_REF
import com.ccc.state.Order
import net.corda.core.contracts.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import java.util.*

// TODO: kyriakos: as is, this flow should be renamed to ReserveStockFlow(?)

/**
 * This flow is to Buy the Stock
 * The Buyer initiates the flow
 * @param orderID The Unique ID of the Order
 */
@InitiatingFlow
@StartableByRPC
class OrderBuyFlow(val orderID: UniqueIdentifier, val amount: Amount<Currency>) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        //Find the order
        val orderStates = serviceHub.vaultService.queryBy(Order::class.java)
        val orderStateAndRef = requireNotNull(orderStates.states.find { it.state.data.linearId == orderID })
        val inputOrder = orderStateAndRef.state.data
        //Create an OrderOutput with current node's id as  a Buyer
        val outputOrder = inputOrder.buy(amount, ourIdentity) //TODO: kyriakos: why copy the price, the price is already there.
        //Create a txBuilder
          // input stateAndref of the order
          // StateAndContract of the order that has the following:
              // output of orderState
              // ORDER_CONTRACT_REF
          // Command to Buy - has the following
             // Command.OrderContract.buy
             // Current participants of the Order + ourIdentity
          // TimeWindow between now and the Order's expiryDate
        val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.get(0))
            .withItems(orderStateAndRef,
                StateAndContract(outputOrder, ORDER_CONTRACT_REF),
                Command(OrderContract.Commands.Buy(), outputOrder.participants.map { it.owningKey } + ourIdentity.owningKey),
                TimeWindow.between(serviceHub.clock.instant(), outputOrder.expiryDateTime)
                )
        // verify the builder
        txBuilder.verify(serviceHub)
        // Self Sign the tx
        val signedInitialTx = serviceHub.signInitialTransaction(txBuilder)
        // Find all the participants minus ourselves
        val everyOneElse = outputOrder.participants - ourIdentity
        // initiate the flow with the above participants and get the Flow sessions
        val flowSessions = everyOneElse.map { initiateFlow(it) }
        // CollectSignaturesFlow with the people
        val signedByAllTx = subFlow(CollectSignaturesFlow(signedInitialTx, flowSessions))
        //return signedByAllTx
        // Finality Flow with the above mentioned people
        return subFlow(FinalityFlow(signedByAllTx, flowSessions))
    }
}

@InitiatedBy(OrderBuyFlow::class)
class OrderBuyFlowResponder(val flowSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                // TODO: Add additional checks here
            }
        }
        subFlow(signedTransactionFlow)
        subFlow(ReceiveFinalityFlow(flowSession, statesToRecord = StatesToRecord.ALL_VISIBLE))
    }
}