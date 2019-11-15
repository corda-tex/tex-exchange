package com.ccc.contract

import com.ccc.contract.StockContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

/**
 * A [LinearState] describing a Stock.
 * @param description Textual description of the  item.
 * @param owner The Party who owns the  item.
 * @param listed Whether or not the  item is listed in an active sale.
 * @param linearId Unique identifier of a StockState object.
 */
@BelongsToContract(StockContract::class)
data class StockState
(
        val description: String,
        val owner: Party,
        val listed: Boolean = false,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
): LinearState {
    override val participants: List<AbstractParty> get() = listOf(owner)

    /**
     * Returns a copy of this StockState which has a new owner and is not listed.
     */
    fun transfer(newOwner: Party): StockState { return copy(owner = newOwner, listed = false) }

    /**
     * Returns a copy of this StockState which is not listed.
     */
    fun delist(): StockState { return copy(listed = false) }

    /**
     * Returns a copy of this StockState which is listed.
     */
    fun list(): StockState { return copy(listed = true) }
}