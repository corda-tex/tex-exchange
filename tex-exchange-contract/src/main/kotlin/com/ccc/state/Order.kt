package com.ccc.state

import com.ccc.contract.OrderContract
import net.corda.core.contracts.*
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.time.Instant
import java.util.*

@BelongsToContract(OrderContract::class)
data class Order(
    val stockId: UniqueIdentifier,
    val stockDescription: String,
    val stockQuantity: Long,
    val price: Amount<Currency>,
    val state: State = State.SELL,
    val expiryDateTime: Instant,
    val seller: Party,
    val buyer: Party?
) : LinearState {
    override val linearId: UniqueIdentifier = UniqueIdentifier()

    override val participants: List<Party> get() = listOfNotNull(seller, buyer)

    /**
     * Returns a copy the order state with a new buying party.
     */
    fun buy(buyer: Party) = this.copy(state = State.BOUGHT, buyer = buyer)

    @CordaSerializable
    enum class State {SELL, BOUGHT}
}