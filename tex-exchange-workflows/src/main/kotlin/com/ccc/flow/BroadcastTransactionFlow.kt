package com.ccc.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

class BroadcastTransactionFlow(
    private val stx: SignedTransaction,
    private val recepients: List<Party>
) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        val partyIterator = recepients.iterator()
        while (partyIterator.hasNext()) {
            val party = partyIterator.next()
            val otherSideSession = initiateFlow(party)
            subFlow(SendTransactionFlow(otherSideSession, stx))
        }
    }
}

/**
 * This is the initiated Flow. The states will be recorded in the vault. You can use the vault query or vault query observer to figure out new listings
 **/
class BroadcastTransactionResponder(private val otherSideSession: FlowSession) : FlowLogic<Unit>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        subFlow(ReceiveTransactionFlow(otherSideSession, statesToRecord = StatesToRecord.ALL_VISIBLE))
    }

}