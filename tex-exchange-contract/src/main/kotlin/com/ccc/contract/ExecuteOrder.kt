package com.ccc.contract

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

//@BelongsToContract(ExecuteOrderContract::class)
data class ExecuteOrder(
    val owner: Party,
    val buyer: Party,
    val orderLinearId: UniqueIdentifier,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {
    override val participants: List<AbstractParty> = listOf(buyer,owner)

}