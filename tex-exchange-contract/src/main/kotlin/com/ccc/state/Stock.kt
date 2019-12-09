package com.ccc.state

import com.ccc.contract.StockContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.finance.contracts.asset.Cash
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.security.PublicKey
import java.util.*

/**
 * @param description Textual description of the  item.
 * @param owner The Party who owns the  item.
 * @param listed Whether or not the  item is listed in an active sale.
 * @param linearId Unique identifier of a StockState object.
 */
@BelongsToContract(StockContract::class)
data class Stock(val description: String,
                 override val owner: AbstractParty,
                 override val amount: Amount<Issued<StockUnit>>,
                 val listed: Boolean = false,
                 val uniqueId: UniqueIdentifier) : FungibleAsset<StockUnit> {

    override val participants: List<AbstractParty> get() = listOf(owner)

    fun split(requestedAmount: Amount<Issued<StockUnit>>): Pair<Stock, Stock> {
        if (requestedAmount.toDecimal().setScale(0) > amount.toDecimal().setScale(0)) throw IllegalArgumentException("The Requested amount is greater than mine")
        val stock1 = copy(amount = requestedAmount)
        val stock2 = copy(amount = amount - requestedAmount)
        return Pair(stock1, stock2)
    }

    override fun withNewOwner(newOwner: AbstractParty): CommandAndState = CommandAndState(Cash.Commands.Move(), copy(owner = newOwner))

        /**
     * Returns a copy of this Stock which is listed.
     */
    fun list(): Stock {
        return copy(listed = true)
    }

    /**
     * Returns a copy of this Stock which is not listed.
     */
    fun delist(): Stock {
        return copy(listed = false)
    }

    // Not to be used...
    override val exitKeys: Collection<PublicKey>
        get() =  listOf(owner.owningKey)
            //TODO("not going to use this one")

    override fun withNewOwnerAndAmount(
        newAmount: Amount<Issued<StockUnit>>,
        newOwner: AbstractParty
    ): FungibleAsset<StockUnit> {
        return this
        //TODO("not going to use this one") //To change body of created functions use File | Settings | File Templates.
    }
}

@CordaSerializable
object StockUnit
