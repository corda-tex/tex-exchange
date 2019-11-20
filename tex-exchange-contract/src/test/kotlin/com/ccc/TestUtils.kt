package com.ccc

import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.LedgerTransaction
import net.corda.testing.core.TestIdentity

val ALICE = TestIdentity(CordaX500Name(organisation = "Alice", locality = "London", country = "GB"))
val BOB = TestIdentity(CordaX500Name(organisation = "Bob", locality = "London", country = "GB"))
val CHARLIE = TestIdentity(CordaX500Name(organisation = "Charlie", locality = "London", country = "GB"))

class DummyContract : Contract {

    companion object {
        @JvmStatic
        val DUMMY_CONTRACT_ID = "com.ccc.DummyContract"
    }

    override fun verify(tx: LedgerTransaction) {
        // Accept all
    }

    interface Commands : CommandData {
        class DummyCommand : TypeOnlyCommandData(), Commands
    }

    @BelongsToContract(DummyContract::class)
    data class DummyState(
        val linearId: UniqueIdentifier = UniqueIdentifier()
    ) : ContractState {
        override val participants: List<AbstractParty>
            get() = listOf()
    }

}