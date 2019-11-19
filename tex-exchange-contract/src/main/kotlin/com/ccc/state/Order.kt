package com.ccc.state

import com.ccc.contract.OrderContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.time.Instant
import java.util.*

@CordaSerializable
enum class Direction { SELL }

@BelongsToContract(OrderContract::class)
data class Order(
    override val linearId: UniqueIdentifier = UniqueIdentifier(),
    val stockLinearId: UniqueIdentifier, //TODO: Is it possible ReferenceState ?
    val stockDescription: String,
    val price: Amount<Currency>,
    val stockUnits: Int,
    val direction: Direction,
    val expiryDateTime: Instant,
    val seller: Party,
    val buyer: Party?
) : LinearState {
    override val participants: List<Party> get() = listOfNotNull(seller, buyer)

    /**
     * Returns a copy the order state with a new buying party.
     */
    fun buy(buyer: Party): Order {
        return this.copy(buyer = buyer)
    }
}