package com.example.state

import com.example.schema.OrderSchemaV1
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

data class Order(
    val amount: Int,
    val buyer: Party,
    val itemOwner: Party,
    val sellOrderReference: UniqueIdentifier,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState, QueryableState {
    override val participants: List<AbstractParty> = listOf(buyer,itemOwner)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is OrderSchemaV1 -> OrderSchemaV1.PersistentOrder(
                    this.itemOwner.name.toString(),
/*                    this.buyer.name.toString(),
                    this.amount,
                    this.sellOrderReference.id.toString(),*/
                    this.linearId.id.toString()
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }
    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(OrderSchemaV1)


}