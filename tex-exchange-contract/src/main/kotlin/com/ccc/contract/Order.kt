package com.ccc.contract

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable
import java.time.Instant

@CordaSerializable
enum class OrderStatus { OPEN, BID_ACCEPTED, SETTLED}

@CordaSerializable
enum class Direction { BUY, SELL }

@BelongsToContract(OrderContract::class)
data class Order(
    val orderId: String,
    val stockId: String,
    val price: Double,
    val stockUnits: Int,
    val direction : Direction,
    val dateTime: Instant,
    val orderStatus: OrderStatus,
    override val linearId: UniqueIdentifier = UniqueIdentifier(), override val participants: List<AbstractParty>
) : LinearState {

}