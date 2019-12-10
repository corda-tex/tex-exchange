//package com.ccc.state
//
//import com.ccc.ALICE
//import com.ccc.BOB
//import net.corda.core.contracts.Amount
//import net.corda.core.contracts.LinearState
//import net.corda.core.contracts.UniqueIdentifier
//import net.corda.core.identity.Party
//import net.corda.finance.POUNDS
//import net.corda.finance.contracts.asset.Cash
//import org.junit.Test
//import java.math.BigDecimal
//import java.time.Instant
//import java.util.*
//import kotlin.test.assertEquals
//import kotlin.test.assertNotEquals
//
//class OrderTests {
//
//    @Test
//    fun hasFieldsOfCorrectType() {
//        Order::class.java.getDeclaredField("stockLinearId")
//        assertEquals(Order::class.java.getDeclaredField("stockLinearId").type, UniqueIdentifier::class.java)
//
//        Order::class.java.getDeclaredField("stockDescription")
//        assertEquals(Order::class.java.getDeclaredField("stockDescription").type, String::class.java)
//
//        Order::class.java.getDeclaredField("price")
//        assertEquals(Order::class.java.getDeclaredField("price").type, Amount::class.java)
//
//        Order::class.java.getDeclaredField("stockUnits")
//        assertEquals(Order::class.java.getDeclaredField("stockUnits").type, Int::class.java)
//
//        Order::class.java.getDeclaredField("direction")
//        assertEquals(Order::class.java.getDeclaredField("direction").type, Direction::class.java)
//
//        Order::class.java.getDeclaredField("expiryDateTime")
//        assertEquals(Order::class.java.getDeclaredField("expiryDateTime").type, Instant::class.java)
//
//        Order::class.java.getDeclaredField("seller")
//        assertEquals(Order::class.java.getDeclaredField("seller").type, Party::class.java)
//
//        Order::class.java.getDeclaredField("buyer")
//        assertEquals(Order::class.java.getDeclaredField("buyer").type, Party::class.java)
//    }
//
//    @Test
//    fun sellerIsParticipant() {
//        val order = Order(
//            UniqueIdentifier(),
//            UniqueIdentifier(),
//            "AMZN 10 units £100",
//            10.POUNDS,
//            10,
//            Direction.SELL,
//            Instant.now(),
//            ALICE.party,
//            BOB.party
//        )
//        assertNotEquals(order.participants.indexOf(ALICE.party), -1)
//    }
//
//    @Test
//    fun buyerIsParticipantIfNotNull() {
//        var order = Order(
//            UniqueIdentifier(),
//            UniqueIdentifier(),
//            "AMZN 10 units £100",
//            10.POUNDS,
//            10,
//            Direction.SELL,
//            Instant.now(),
//            ALICE.party,
//            BOB.party
//        )
//        assertNotEquals(order.participants.indexOf(BOB.party), -1)
//
//        order = Order(
//            UniqueIdentifier(),
//            UniqueIdentifier(),
//            "AMZN 10 units £100",
//            10.POUNDS,
//            10,
//            Direction.SELL,
//            Instant.now(),
//            ALICE.party,
//            null
//        )
//        assertEquals(order.participants.indexOf(BOB.party), -1)
//    }
//
//    @Test
//    fun isLinearState() {
//        assert(LinearState::class.java.isAssignableFrom(Order::class.java))
//    }
//
//    @Test
//    fun hasLinearIdFieldOfCorrectType() {
//        // Does the linearId field exist?
//        Order::class.java.getDeclaredField("linearId")
//        // Is the linearId field of the correct type? Let us consider linearId as OrderID
//        assertEquals(Order::class.java.getDeclaredField("linearId").type, UniqueIdentifier::class.java)
//    }
//
//    @Test
//    fun checkParameterOrdering() {
//        val fields = Order::class.java.declaredFields
//        val linearIdIdx = fields.indexOf(Order::class.java.getDeclaredField("linearId"))
//        val stockIdx = fields.indexOf(Order::class.java.getDeclaredField("stockLinearId"))
//        val descIdx = fields.indexOf(Order::class.java.getDeclaredField("stockDescription"))
//        val priceIdx = fields.indexOf(Order::class.java.getDeclaredField("price"))
//        val stockUnitsIdx = fields.indexOf(Order::class.java.getDeclaredField("stockUnits"))
//        val directionIdx = fields.indexOf(Order::class.java.getDeclaredField("direction"))
//        val expiryDateTimeIdx = fields.indexOf(Order::class.java.getDeclaredField("expiryDateTime"))
//        val sellerIdx = fields.indexOf(Order::class.java.getDeclaredField("seller"))
//        val buyerIdx = fields.indexOf(Order::class.java.getDeclaredField("buyer"))
//
//        assert(linearIdIdx < stockIdx)
//        assert(stockIdx < descIdx)
//        assert(descIdx < priceIdx)
//        assert(priceIdx < stockUnitsIdx)
//
//        assert(stockUnitsIdx < directionIdx)
//        assert(directionIdx < expiryDateTimeIdx)
//        assert(expiryDateTimeIdx < sellerIdx)
//        assert(sellerIdx < buyerIdx)
//    }
//
//    @Test
//    fun checkBuyMethod() {
//        val order = Order(
//            UniqueIdentifier(),
//            UniqueIdentifier(),
//            "AMZN 10 units £100",
//            10.POUNDS,
//            10,
//            Direction.SELL,
//            Instant.now(),
//            ALICE.party,
//            null
//        )
//        val newOrder = order.buy(100.POUNDS,  BOB.party)
//        assertEquals(BOB.party, newOrder.buyer)
//        assertEquals(100.POUNDS, newOrder.price)
//    }
//}
