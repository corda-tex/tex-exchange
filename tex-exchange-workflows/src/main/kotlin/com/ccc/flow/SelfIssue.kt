package com.ccc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ccc.contract.StockContract
import com.ccc.contract.StockContract.Companion.STOCK_CONTRACT_REF
import com.ccc.state.Stock
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.ProgressTracker
import net.corda.finance.POUNDS
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.contracts.utils.sumCash
import net.corda.finance.flows.AbstractCashFlow
import net.corda.finance.flows.CashIssueFlow
import java.util.*

/**
 * This is the flow that self issues the Stock to the ledger
 * The flow returns the unique id of the Stock, after a due notarization
 */
object SelfIssue {

    @StartableByRPC
    class SelfIssueStockFlow(
        val description: String,
        val code: String,
        val count: Int
    ) : FlowLogic<UniqueIdentifier>() {

        override val progressTracker = ProgressTracker()

        @Suspendable
        override fun call(): UniqueIdentifier {
            val stock = Stock(description, code, owner = ourIdentity, count = count) //Create a Stock State with the invoker's identity
            val command = Command(StockContract.Commands.Issue(), listOf(ourIdentity.owningKey))
            //TO REMEMBER : When we use the Cordite's notary, the following line has to be altered to reflect that. We may use Cordite's notary for metering
            val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
            txBuilder.withItems(StateAndContract(stock, STOCK_CONTRACT_REF), command)
            txBuilder.verify(serviceHub)
            val stx = serviceHub.signInitialTransaction(txBuilder)
            return subFlow(FinalityFlow(stx, emptyList())).tx.outputsOfType(Stock::class.java).single().linearId
        }
    }

    // Money "agnostic" used: MONEY_UNITS
    class SelfIssueCashFlow(val units: Int) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            val issueRef = OpaqueBytes.of(0)
            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            subFlow(CashIssueFlow(units.MONEY_UNITS, issueRef, notary))
        }
    }

    class MergeCashFlow(progressTracker: ProgressTracker): AbstractCashFlow<Unit>(progressTracker) {
        constructor(): this(tracker())
        @Suspendable
        override fun call() {
            val me = ourIdentity
            val cashStates = serviceHub.vaultService.queryBy(Cash.State::class.java).states
            val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
            cashStates.forEach { txBuilder.addInputState(it) } // Add all the existing cash states.
            try {
                val cashSum = cashStates.map { it.state.data }.sumCash()
                txBuilder.addOutputState(Cash.State(cashSum, me))
                txBuilder.addCommand(Cash.Commands.Move(), me.owningKey)
                val stx = serviceHub.signInitialTransaction(txBuilder)
                finaliseTx(stx, emptySet(), "Unable to notarise issue").tx.outputsOfType(Cash.State::class.java).single()
            } catch(e: UnsupportedOperationException) {
                // thrown by sumCash()
            }
        }
    }

    val Int.MONEY_UNITS: Amount<Currency> get() = POUNDS
}