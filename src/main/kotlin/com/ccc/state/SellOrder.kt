package com.example.state

import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.time.Instant


data class SellOrder(
    val itemName: String,
    val ItemDescription: String,
    val startPrice: Int,
    val ExpiryDate: Instant,
    val itemOwner: Party,
    val saleWinner: Party ?= null,
    val highestBid: Int,
    val SaleActive: Boolean,
    val SaleInterestedParticipants: List<Party>,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState, SchedulableState {
    override val participants: List<AbstractParty> = if (saleWinner!=null) listOf(itemOwner,saleWinner) else listOf(itemOwner)

    override fun nextScheduledActivity(thisStateRef: StateRef, flowLogicRefFactory: FlowLogicRefFactory): ScheduledActivity? {
        //return ScheduledActivity(flowLogicRefFactory.create(EndSale::class.java,linearId.toString()), ExpiryDate)
        TODO("To Do")
    }
}