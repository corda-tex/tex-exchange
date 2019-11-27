package com.ccc.state

import com.ccc.contract.OrderContract2
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.math.BigDecimal
import java.time.Instant

@BelongsToContract(OrderContract2::class)
data class Order2(
    override val linearId: UniqueIdentifier = UniqueIdentifier(),
    val ticker: String,
    val units: Int,
    val price: BigDecimal,
    val direction: Direction,
    val expires: Instant,
    val seller: Party,
    val buyers: Set<Party> = mutableSetOf()
) : LinearState {
    override val participants: List<AbstractParty> get() = buyers.plus(seller).toList()
}
