package com.ccc.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

class BroadcastTransactionFlow(
    private val stx: SignedTransaction,
    private val recipients: List<Party>
) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        val partyIterator = recipients.iterator()
        while (partyIterator.hasNext()) {
            val party = partyIterator.next()
            val otherSideSession = initiateFlow(party)
            subFlow(SendTransactionFlow(otherSideSession, stx))
        }
    }
}