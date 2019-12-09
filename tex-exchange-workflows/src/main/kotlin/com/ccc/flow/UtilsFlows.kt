package com.ccc.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.contextLogger
import net.corda.finance.GBP
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.flows.AbstractCashFlow

// TODO: make the below flows generic to support both Cash and Stock
object UtilsFlows {

    class MergeCashFlow(progressTracker: ProgressTracker): AbstractCashFlow<Unit>(progressTracker) {
        constructor(): this(tracker())

        private companion object {
            private val log = contextLogger()
        }

        @Suspendable
        override fun call() {
            val me = ourIdentity
            val cashStates = serviceHub.vaultService.queryBy(Cash.State::class.java).states
            if (cashStates.isEmpty()) {
                log.info("There are no cash states in my vault to merge!")
                return
            }

            val token = cashStates[0].state.data.amount.token
            var cashSum = Amount.zero(token)
            val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
            for (cashState in cashStates) {
                txBuilder.addInputState(cashState)
                cashSum += cashState.state.data.amount
            }
            txBuilder.addOutputState(Cash.State(cashSum, me))
            txBuilder.addCommand(Cash.Commands.Move(), me.owningKey)
            val stx = serviceHub.signInitialTransaction(txBuilder)
            finaliseTx(stx, emptySet(), "Unable to notarise issue").tx.outputsOfType(Cash.State::class.java).single()
        }
    }

    // 1. Split Cash : 1 cash state -> 2 cash states -> return the cash state we requested
    class SplitAndGetCashFlow(private val units: Int, progressTracker: ProgressTracker): AbstractCashFlow<Unit>(progressTracker) {
        constructor(units: Int): this(units, tracker())
        @Suspendable
        override fun call() {
            // Get all Cash states and iterate through them to find one big enough to split.
            val cashStates = serviceHub.vaultService.queryBy(Cash.State::class.java).states
            var cashStateToReturn: StateAndRef<Cash.State>? = null
            for (cashState in cashStates) {
                if (cashState.state.data.amount.toDecimal().setScale(0) == units.toBigDecimal()) { // no need to split it, just return this one.
                    cashStateToReturn = cashState
                } else if (cashState.state.data.amount.toDecimal().setScale(0) > units.toBigDecimal()) { // do splitting.
                    //cashStateToReturn = doSplitting(cashState, units)
                } else {
                    // Maybe a Merge would help here

                }
            }
        }

        // Split and return the Cash.State we want to use.
//        private fun doSplitting(cashState: StateAndRef<Cash.State>, units: Int): StateAndRef<Cash.State> {
//            val me = ourIdentity
//
//
//        }

    }
}