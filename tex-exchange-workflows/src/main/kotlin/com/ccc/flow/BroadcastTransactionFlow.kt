package com.ccc.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
class BroadcastTransactionFlow(
    private val stx: SignedTransaction,
    private val recepients: List<Party>
) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        recepients.stream().forEach {
            val otherSideSession = initiateFlow(it)
            subFlow(SendTransactionFlow(otherSideSession, stx))
        }
    }
}

/**
 * This is the initiated Flow. The states will be recorded in the vault. You can use the vault query or vault query observer to figure out new listings
 **/
@InitiatedBy(BroadcastTransactionFlow::class)
class BroadcastTransactionResponder(private val otherSideSession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        subFlow(ReceiveTransactionFlow(otherSideSession, statesToRecord = StatesToRecord.ALL_VISIBLE))
    }

}