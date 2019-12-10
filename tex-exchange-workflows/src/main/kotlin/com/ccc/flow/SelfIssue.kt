package com.ccc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ccc.contract.StockContract
import com.ccc.contract.StockContract.Companion.STOCK_CONTRACT_REF
import com.ccc.state.Stock
import com.ccc.state.StockUnit
import net.corda.core.contracts.*
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.ProgressTracker
import net.corda.finance.POUNDS
import net.corda.finance.flows.CashIssueFlow
import java.util.*

/**
 * This is the flow that self issues the Stock to the ledger
 * The flow returns the unique id of the Stock, after a due notarization
 */
object SelfIssue {

    @StartableByRPC
    class SelfIssueStockFlow(private val stockId: UniqueIdentifier? = null,
                             val description: String,
                             private val quantity: Long) : FlowLogic<UniqueIdentifier>() {

        override val progressTracker = ProgressTracker()

        @Suspendable
        override fun call(): UniqueIdentifier {
            val issueRef = OpaqueBytes.of(0)
            val me = ourIdentity.ref(issueRef)
            val token = Issued(me, StockUnit)
            val zeroAmount = Amount.zero(token)
            val stock = Stock(
                stockId ?: UniqueIdentifier(), // existing or new stock.
                description,
                ourIdentity,
                zeroAmount.copy(quantity = quantity),
                listed = false
            )

            val command = Command(StockContract.Commands.Issue(), listOf(ourIdentity.owningKey))
            //TO REMEMBER : When we use the Cordite's notary, the following line has to be altered to reflect that. We may use Cordite's notary for metering
            val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
            txBuilder.withItems(StateAndContract(stock, STOCK_CONTRACT_REF), command)
            txBuilder.verify(serviceHub)
            val stx = serviceHub.signInitialTransaction(txBuilder)
            return subFlow(FinalityFlow(stx, emptyList())).tx.outputsOfType(Stock::class.java).single().stockId
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

    val Int.MONEY_UNITS: Amount<Currency> get() = POUNDS
}