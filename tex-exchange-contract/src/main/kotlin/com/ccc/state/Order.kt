package com.ccc.state

import com.ccc.contract.OrderContract
import net.corda.core.contracts.*
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.lang.IllegalStateException
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
    val buyer: Party?,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {

    override val participants: List<Party> get() = listOfNotNull(seller, buyer)

    /**
     * Returns a copy the order state with a new buying party.
     */
    fun buy(buyer: Party) = this.copy(state = State.BOUGHT, buyer = buyer)

    fun settle(): Order {
        buyer?: throw IllegalStateException("Buyer cannot be null when settling")
        return copy(state = State.SETTLED) // buyer was set when it was bought.
    }

    @CordaSerializable
    enum class State {SELL, BOUGHT, SETTLED}
}