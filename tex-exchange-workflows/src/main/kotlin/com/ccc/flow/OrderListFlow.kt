package com.ccc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ccc.contract.OrderContract
import com.ccc.contract.StockContract
import com.ccc.state.Direction
import com.ccc.state.Order
import com.ccc.state.Stock
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
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
    val stockID: UniqueIdentifier,
    val price: Amount<Currency>,
    val stockUnits: Int,
    val expiry: Instant
) : FlowLogic<SignedTransaction>() {

    override val progressTracker: ProgressTracker?
        get() = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        //Find the Stock from the vault
        val stock = serviceHub.vaultService.queryBy(Stock::class.java)
        val stockStateAndRef = stock.states.find { it.state.data.linearId == stockID }
            ?: throw IllegalArgumentException("Stock with '$stockID' does not exist")
        //Check if the Stock is open for listing. Else, throw an Exception
        if (stockStateAndRef.state.data.listed) {
            throw IllegalArgumentException("The stock is already listed")
        }
        //Create a Sell Order
        val inputOrder = Order(
            stockLinearId = stockID,
            stockDescription = stockStateAndRef.state.data.description,
            price = price,
            stockUnits = stockUnits,
            direction = Direction.SELL,
            expiryDateTime = expiry,
            seller = ourIdentity,
            buyer = null
        )
        //Create a Stock state with listed set to be true
        val outputStock = stockStateAndRef.state.data.copy(listed = true)
        //Create a Command to list the stock
        val commandStockList = Command(StockContract.Commands.List(), ourIdentity.owningKey)
        //Create a Command to list the Sell order
        val commandOrderList = Command(OrderContract.Commands.List(), ourIdentity.owningKey)
        //Create a Transaction Builder with the following items
        // Stock stateAndRef (old)
        // Stock List command
        // StateAndContract for Stock - Tell the contract ID and the outputState
        // the new Output Stock State
        // and
        // Stock Contract ID
        // Sell Order Command with the first time Sell Order state
        // StateAndContract for Sell Order - Tell the contract ID and the outputState
        // the first Order State
        // and
        // Order Contract ID
        // Set a Time window between now and the expiry date of the sell order
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val txBuilder = TransactionBuilder(notary)
            .withItems(
                stockStateAndRef,
                commandStockList,
                StateAndContract(outputStock, StockContract.STOCK_CONTRACT_REF),
                commandOrderList,
                StateAndContract(inputOrder, OrderContract.ORDER_CONTRACT_REF),
                TimeWindow.between(serviceHub.clock.instant(), inputOrder.expiryDateTime)
            )
        //Verify the builder with the serviceHub before signing the Tx (This will run the contracts)
        txBuilder.verify(serviceHub)
        //Create a Signed Tx
        val signedInitialTx = serviceHub.signInitialTransaction(txBuilder)
        //Create a FinalityFlow and also BroadcastTx to all the parties
        val tx = subFlow(FinalityFlow(signedInitialTx, emptyList()))
        val broadcastList =
            serviceHub.networkMapCache.allNodes.map { node -> node.legalIdentities.first() } - inputOrder.seller - notary
        subFlow(BroadcastTransactionFlow(tx, broadcastList))
        return tx

     //   return subFlow(FinalityFlow(signedInitialTx, emptyList()))

        //TODO: Take the output state of the Sell Order and figure out the UUID of the sell order
    }
}


/*@InitiatedBy(OrderListFlow::class)
class OrderListFlowResponder(val otherSideSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signedTransactionFlow = object : SignTransactionFlow(otherSideSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {

            }
        }

        subFlow(ReceiveFinalityFlow(otherSideSession, statesToRecord = StatesToRecord.ALL_VISIBLE))
        subFlow(BroadcastTransactionResponder(otherSideSession))
    }

}*/
