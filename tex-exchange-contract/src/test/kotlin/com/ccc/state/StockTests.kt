package com.ccc.state

import com.ccc.ALICE
import com.ccc.BOB
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals

class StockTests {

    @Test
    fun hasFieldsOfCorrectType() {
        Stock::class.java.getDeclaredField("description")
        assertEquals(Stock::class.java.getDeclaredField("description").type, String::class.java)

        Stock::class.java.getDeclaredField("owner")
        assertEquals(Stock::class.java.getDeclaredField("owner").type, Party::class.java)

        Stock::class.java.getDeclaredField("listed")
        assertEquals(Stock::class.java.getDeclaredField("listed").type, Boolean::class.java)
    }

    @Test
    fun ownerIsParticipant() {
        val stock = Stock("description", "code", ALICE.party)
        assertEquals(stock.participants.indexOf(ALICE.party), 0)
    }

    @Test
    fun isLinearState() {
        assert(LinearState::class.java.isAssignableFrom(Stock::class.java))
    }

    @Test
    fun hasLinearIdFieldOfCorrectType() {
        // Does the linearId field exist?
        Stock::class.java.getDeclaredField("linearId")
        // Is the linearId field of the correct type?
        assertEquals(Stock::class.java.getDeclaredField("linearId").type, UniqueIdentifier::class.java)
    }


    @Ignore
    fun checkParameterOrdering() {
        val fields = Stock::class.java.declaredFields
        val linearIdIdx = fields.indexOf(Stock::class.java.getDeclaredField("linearId"))
        val descriptionIdx = fields.indexOf(Stock::class.java.getDeclaredField("description"))
        val ownerIdx = fields.indexOf(Stock::class.java.getDeclaredField("owner"))
        val listedIdx = fields.indexOf(Stock::class.java.getDeclaredField("listed"))
        assert(linearIdIdx < descriptionIdx)
        assert(descriptionIdx < ownerIdx)
        assert(ownerIdx < listedIdx)
    }

    @Test
    fun checkListHelperMethod() {
        val stock = Stock("description", "code", ALICE.party)
        val listedStock = stock.list()
        assertEquals(true, listedStock.listed)
        assertEquals(stock, listedStock.copy(listed = false))
    }

    @Test
    fun checkTransferHelperMethod() {
        val appleStock = Stock("description", "code", ALICE.party, listed = true)
        val newAppleStock = appleStock.transfer(BOB.party)
        assertEquals(BOB.party, newAppleStock.owner)
        assertEquals(false, newAppleStock.listed)
        assertEquals(appleStock, newAppleStock.copy(owner = appleStock.owner, listed = true))
    }

    @Test
    fun checkDelistHelperMethod() {
        val appleStock = Stock("description", "code", ALICE.party, listed = true)
        val newAppleStock = appleStock.delist()
        assertEquals(false, newAppleStock.listed)
        assertEquals(appleStock, newAppleStock.copy(listed = true))
    }
}
