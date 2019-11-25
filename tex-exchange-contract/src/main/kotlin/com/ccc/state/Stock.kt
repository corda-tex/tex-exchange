package com.ccc.state

import com.ccc.contract.StockContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

/**
 * A [LinearState] describing a Stock.
 * @param description Textual description of the  item.
 * @param owner The Party who owns the  item.
 * @param listed Whether or not the  item is listed in an active sale.
 * @param linearId Unique identifier of a StockState object.
 *
 * TODO Should create OwnedStock from Stock, Stock data is agnostic from ownership data
 *
 */
@BelongsToContract(StockContract::class)
data class Stock
    (
    override val linearId: UniqueIdentifier = UniqueIdentifier(),
    val description: String,
    val code: String,
    val owner: Party,
    val count: Int = 1,
    val listed: Boolean = false
) : LinearState {
    override val participants: List<AbstractParty> get() = listOf(owner)

    /**
     * Returns a copy of this Stock which is listed.
     */
    fun list(): Stock {
        return copy(listed = true)
    }

    /**
     * Returns a copy of this Stock which has a new owner and is not listed.
     */
    fun transfer(newOwner: Party, count: Int): Stock {
        return copy(owner = newOwner, count = count, listed = false)
    }

    /**
     * Returns a copy of this Stock which is not listed.
     */
    fun deList(): Stock {
        return copy(listed = false)
    }


}