package com.ccc.state

import com.ccc.contract.StockContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable
import java.security.PublicKey

/**
 * @param description Textual description of the  item.
 * @param owner The Party who owns the  item.
 * @param orderId Whether or not the  item is listed in an active sale.
 * @param linearId Unique identifier of a StockState object.
 */
@BelongsToContract(StockContract::class)
data class Stock(
    val stockId: UniqueIdentifier,
    val description: String,
    override val owner: AbstractParty,
    override val amount: Amount<Issued<StockUnit>>,
    val orderId: UniqueIdentifier? = null // if a Stock gets listed it gets the Order's id.
) : FungibleAsset<StockUnit> {

    override val participants: List<AbstractParty> get() = listOf(owner)

    override fun withNewOwner(newOwner: AbstractParty): CommandAndState = CommandAndState(StockContract.Commands.Transfer(), copy(owner = newOwner))

        /**
     * Returns a copy of this Stock which is listed.
     */
    fun list(orderId: UniqueIdentifier): Stock {
        return copy(orderId = orderId)
    }

    /**
     * Returns a copy of this Stock which is not listed.
     */
    fun delist(): Stock {
        return copy(orderId = null)
    }

    fun isListed() = orderId != null

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
