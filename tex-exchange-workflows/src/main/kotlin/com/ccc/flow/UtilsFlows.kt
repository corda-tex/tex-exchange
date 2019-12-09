package com.ccc.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.contextLogger
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.flows.AbstractCashFlow

// TODO: make the below flows generic to support both Cash and Stock
object UtilsFlows {

    class MergeCashFlow(progressTracker: ProgressTracker): AbstractCashFlow<Boolean>(progressTracker) {
        constructor(): this(tracker())

        private companion object {
            private val log = contextLogger()
        }

        @Suspendable
        override fun call(): Boolean {
            val me = ourIdentity
            val cashStates = serviceHub.vaultService.queryBy(Cash.State::class.java).states
            if (cashStates.isEmpty()) {
                log.info("There are no cash states in my vault to merge. Returning...")
                return false
            }

            if (cashStates.size == 1) {
                log.info("There is only one cash state. No need for merging. Returning...")
                return false
            }

            val token = cashStates[0].state.data.amount.token
            var cashSum = Amount.zero(token)
            val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
            for (cashState in cashStates) {
                txBuilder.addInputState(cashState)
                cashSum += cashState.state.data.amount
            }
            txBuilder.addOutputState(Cash.State(cashSum, me))
            .addCommand(Cash.Commands.Move(), me.owningKey)
            .verify(serviceHub)

            val stx = serviceHub.signInitialTransaction(txBuilder)
            finaliseTx(stx, emptySet(), "Unable to notarise issue").tx.outputsOfType(Cash.State::class.java).single()
            return true
        }
    }

    // 1. Split Cash : 1 cash state -> 2 cash states -> return the cash state we requested
    class SplitAndGetCashFlow(private val units: Int, progressTracker: ProgressTracker): AbstractCashFlow<Cash.State?>(progressTracker) {
        constructor(units: Int): this(units, tracker())
        @Suspendable
        override fun call(): Cash.State? {
            // Get all Cash states and iterate through them to find one big enough to split.
            val cashStates = serviceHub.vaultService.queryBy(Cash.State::class.java).states
            var cashStateToReturn: Cash.State? = null
            for (cashState in cashStates) {
                if (cashState.state.data.amount.toDecimal().setScale(0) == units.toBigDecimal()) { // no need to split it, just return this one.
                    cashStateToReturn = cashState.state.data
                } else if (cashState.state.data.amount.toDecimal().setScale(0) > units.toBigDecimal()) { // do splitting.
                    cashStateToReturn = splitAndGetCash(cashState, units)
                } else { }
            }

            if (cashStateToReturn == null) { // merge and try again.
                val mergeDone = subFlow(MergeCashFlow())
                if (mergeDone) cashStateToReturn = call()
            }

            return cashStateToReturn
        }

        // Split and return the Cash.State we want to use.
        @Suspendable
        private fun splitAndGetCash(cashState: StateAndRef<Cash.State>, units: Int): Cash.State {
            val me = ourIdentity
            val token = cashState.state.data.amount.token
            val initialAmount =  cashState.state.data.amount
            val unitsAmount = Amount.fromDecimal(units.toBigDecimal(), token)
            val unitsCashState = Cash.State(unitsAmount, me)
            val complementaryCashState = Cash.State(initialAmount - unitsAmount, me)

            val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
            txBuilder.addInputState(cashState)
            .addOutputState(unitsCashState)
            .addOutputState(complementaryCashState)
            .addCommand(Cash.Commands.Move(), me.owningKey)
            .verify(serviceHub)

            val stx = serviceHub.signInitialTransaction(txBuilder)
            val ftx =  finaliseTx(stx, emptySet(), "Unable to notarise issue")
            return ftx.tx.outputsOfType(Cash.State::class.java).apply { assert(size == 2) }[0] // careful to always put unitsCashState first in txBuilder.
        }

    }
}