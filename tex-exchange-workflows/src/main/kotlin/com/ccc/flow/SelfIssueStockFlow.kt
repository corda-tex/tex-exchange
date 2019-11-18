package com.ccc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ccc.contract.StockContract
import com.ccc.contract.StockContract.Companion.STOCK_CONTRACT_REF
import com.ccc.state.Stock
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

/**
 * This is the flow that self issues the Stock to the ledger
 * The flow returns the unique id of the Stock, after a due notarization
 */
@InitiatingFlow
@StartableByRPC
class SelfIssueStockFlow(val description: String) : FlowLogic<UniqueIdentifier>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): UniqueIdentifier {
        //Create a Stock State with the invoker's identity
        val stock = Stock(description = description, owner = ourIdentity)
        //Create an Issue Stock Command with the invoker's key as the signer
        val command = Command(StockContract.Commands.Issue(), listOf(ourIdentity.owningKey))
        //Use a txBuilder to build a transaction of the Stock and Issue Command
        //TO REMEMBER : When we use the Cordite's notary, the following line has to be altered to reflect that. We may use Cordite's notary for metering
        val builder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
        builder.withItems(StateAndContract(stock, STOCK_CONTRACT_REF), command)
        builder.verify(serviceHub)
        //Get the tx verified using the Issue command
        builder.verify(serviceHub)
        //Sign the tx
        val stx = serviceHub.signInitialTransaction(builder)
        //FinalityFlow - let it complete and get the output stock
        subFlow(FinalityFlow(stx, emptyList())).tx.outputsOfType(Stock::class.java).single()
        //Return the stock's unique ID
        return stock.linearId
    }
}