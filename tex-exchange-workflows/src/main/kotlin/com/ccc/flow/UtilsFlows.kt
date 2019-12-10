package com.ccc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ccc.contract.StockContract
import com.ccc.contract.StockContract.Companion.STOCK_CONTRACT_REF
import com.ccc.state.Stock
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
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
    class GetCashFlow(private val units: Int, progressTracker: ProgressTracker): AbstractCashFlow<StateAndRef<Cash.State>?>(progressTracker) {
        constructor(units: Int) : this(units, tracker())

        @Suspendable
        override fun call(): StateAndRef<Cash.State>? {
            // Get all Cash states and iterate through them to find one big enough to split.
            val cashStates = serviceHub.vaultService.queryBy(Cash.State::class.java).states
            var cashStateToReturn: StateAndRef<Cash.State>? = null
            for (cashState in cashStates) {
                if (cashState.state.data.amount.toDecimal().setScale(0) == units.toBigDecimal()) { // no need to split it, just return this one.
                    cashStateToReturn = cashState
                } else if (cashState.state.data.amount.toDecimal().setScale(0) > units.toBigDecimal()) { // do splitting.
                    cashStateToReturn = splitAndGetCash(cashState, units)
                } else {
                }
            }

            if (cashStateToReturn == null) { // merge and try again.
                val mergeDone = subFlow(MergeCashFlow())
                if (mergeDone) cashStateToReturn = call()
            }

            return cashStateToReturn
        }

        // Split and return the Cash.State we want to use.
        @Suspendable
        private fun splitAndGetCash(cashState: StateAndRef<Cash.State>, units: Int): StateAndRef<Cash.State> {
            val me = ourIdentity
            val token = cashState.state.data.amount.token
            val initialAmount = cashState.state.data.amount
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
            val ftx = finaliseTx(stx, emptySet(), "Unable to notarise issue")
            return ftx.tx.outRef(0) // careful to always put unitsCashState first in txBuilder.
        }
    }

    class MergeStockFlow(private val stockId: UniqueIdentifier): FlowLogic<Boolean>() {

        private companion object {
            private val log = contextLogger()
        }

        @Suspendable
        override fun call(): Boolean {
            val me = ourIdentity
            val stockStates = serviceHub.vaultService.queryBy(Stock::class.java).states
                .filter { it.state.data.stockId == stockId && !it.state.data.isListed() }
            if (stockStates.isEmpty()) {
                log.info("There are no stock states in my vault to merge. Returning...")
                return false
            }

            if (stockStates.size == 1) {
                log.info("There is only one stock state. No need for merging. Returning...")
                return false
            }

            val notary = serviceHub.networkMapCache.notaryIdentities.first()

            // Populate stockBuckets first
            val token = stockStates[0].state.data.amount.token
            var stockSum = Amount.zero(token)
            val txBuilder = TransactionBuilder(notary = notary)
            for (stockState in stockStates) {
                txBuilder.addInputState(stockState)
                stockSum += stockState.state.data.amount
            }
            val stockOut = stockStates[0].state.data.copy(amount = stockSum)
            txBuilder
                .addOutputState(stockOut, STOCK_CONTRACT_REF)
                .addCommand(StockContract.Commands.Merge(), me.owningKey)
                .verify(serviceHub)
            val stx = serviceHub.signInitialTransaction(txBuilder)
            subFlow(FinalityFlow(stx, emptySet<FlowSession>())).tx.outputsOfType(Stock::class.java).single()
            return true
        }
    }

    class GetStockFlow(private val stockId: UniqueIdentifier, private val quantity: Long): FlowLogic<StateAndRef<Stock>?>() {

        @Suspendable
        override fun call(): StateAndRef<Stock>? {
            // Go simple this time: call MergeStockFlow at the beginning.
            subFlow(MergeStockFlow(stockId))
            val stockToReturn: StateAndRef<Stock>?
            val stockStates = serviceHub.vaultService.queryBy(Stock::class.java).states
                .filter { it.state.data.stockId == stockId && !it.state.data.isListed() }
            if (stockStates.isEmpty() || stockStates.size > 1) {
                return null
            }
            val stockState = stockStates[0]
            when {
                quantity.toBigDecimal() > stockState.state.data.amount.toDecimal().setScale(0) -> stockToReturn = null
                quantity.toBigDecimal() == stockState.state.data.amount.toDecimal().setScale(0) -> stockToReturn = stockState
                else -> stockToReturn = splitAndGetStock(stockState, quantity)
            }
            return stockToReturn
        }

        // Split and return the StateAndRef<Stock> we want to use.
        @Suspendable
        private fun splitAndGetStock(stockState: StateAndRef<Stock>, quantity: Long): StateAndRef<Stock> {
            val me = ourIdentity
            val inStock = stockState.state.data
            val token = inStock.amount.token
            val initialAmount = inStock.amount
            val unitsAmount = Amount.fromDecimal(quantity.toBigDecimal(), token)
            val unitsStockState = inStock.copy(amount = unitsAmount, owner = me)
            val complementaryStockState = inStock.copy(amount = initialAmount - unitsAmount, owner = me)

            val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
            txBuilder.addInputState(stockState)
                .addOutputState(unitsStockState)
                .addOutputState(complementaryStockState)
                .addCommand(StockContract.Commands.Split(), me.owningKey)
                .verify(serviceHub)

            val stx = serviceHub.signInitialTransaction(txBuilder)
            return subFlow(FinalityFlow(stx, emptySet<FlowSession>())).tx.outRef(0) // careful to always put unitsCashState first in txBuilder.
        }

    }
}