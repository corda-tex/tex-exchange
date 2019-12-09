package com.ccc.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.contracts.utils.sumCash
import net.corda.finance.flows.AbstractCashFlow

object UtilsFlows {
    class MergeCashFlow(progressTracker: ProgressTracker): AbstractCashFlow<Unit>(progressTracker) {
        constructor(): this(tracker())
        @Suspendable
        override fun call() {
            val me = ourIdentity
            val cashStates = serviceHub.vaultService.queryBy(Cash.State::class.java).states
            val txBuilder =
                TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
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

}