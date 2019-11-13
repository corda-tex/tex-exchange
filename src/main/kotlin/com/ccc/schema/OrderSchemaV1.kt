package com.ccc.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * The family of schemas for TradeState.
 */
object OrderSchema

/**
 * An TradeState schema.
 */
object OrderSchemaV1 : MappedSchema(
        schemaFamily = OrderSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentOrder::class.java)) {
    @Entity
    @Table(name = "order_states")
    class PersistentOrder(
            @Column(name = "itemOwner")
            var itemOwner: String,

            // TODO: Add more fields
            @Column(name = "linear_id")
            var linearId: String

    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "")
    }
}