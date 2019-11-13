package com.ccc.state

import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.time.Instant
import java.time.LocalDateTime

@CordaSerializable
enum class OrderStatus { OPEN, BID_ACCEPTED, SETTLED}

@CordaSerializable
enum class Direction { BUY, SELL }

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