//package com.ccc.contract
//
//
//import com.ccc.ALICE
//import com.ccc.BOB
//import com.ccc.DummyContract
//import com.ccc.DummyContract.Companion.DUMMY_CONTRACT_ID
//import com.ccc.contract.OrderContract.Companion.ORDER_CONTRACT_REF
//import com.ccc.state.Direction
//import com.ccc.state.Order
//import com.ccc.state.Stock
//import net.corda.core.contracts.*
//import net.corda.finance.POUNDS
//import net.corda.testing.node.ledger
//import net.corda.testing.node.MockServices
//import org.junit.Ignore
//
//import org.junit.Test
//
//import java.time.Instant
//
//class OrderBuyTests {
//
//    var ledgerServices = MockServices(
//            listOf("com.ccc.contract"))
//
//    @Ignore
//    fun mustHandleBuyCommands() {
//        val expiry = Instant.now().plusSeconds(3600)
//        val stock = Stock("description", "code", ALICE.party)
//        val orderInput = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiry, ALICE.party, null
//        )
//        val orderOutput = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiry, ALICE.party, BOB.party
//        )
//
//        ledgerServices.ledger {
//            transaction {
//                input(ORDER_CONTRACT_REF, orderInput)
//                output(ORDER_CONTRACT_REF, orderOutput)
//                command(listOf(ALICE.publicKey, BOB.publicKey), DummyContract.Commands.DummyCommand())
//                timeWindow(TimeWindow.between(Instant.now(), expiry))
//                this `fails with` "Required com.ccc.contract.OrderContract.Commands command"
//            }
//            transaction {
//                input(ORDER_CONTRACT_REF, orderInput)
//                output(ORDER_CONTRACT_REF, orderOutput)
//                timeWindow(TimeWindow.between(Instant.now(), expiry))
//                command(listOf(ALICE.publicKey, BOB.publicKey), OrderContract.Commands.Buy())
//                this.verifies()
//            }
//        }
//    }
//
//    @Ignore
//    fun mustHaveOneInputOneOutputState() {
//        val expiry = Instant.now().plusSeconds(3600)
//        val stock = Stock("description", "code", ALICE.party)
//        val orderInput = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiry, ALICE.party, null
//        )
//        val orderOutput = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiry, ALICE.party, BOB.party
//        )
//        ledgerServices.ledger {
//            transaction {
//                input(ORDER_CONTRACT_REF, orderInput)
//                input(ORDER_CONTRACT_REF, orderInput)
//                output(ORDER_CONTRACT_REF, orderOutput)
//                command(listOf(ALICE.publicKey, BOB.publicKey), OrderContract.Commands.Buy())
//                timeWindow(TimeWindow.between(Instant.now(), expiry))
//                this `fails with` "A Buy transaction should only have one input state"
//            }
//
//            transaction {
//                input(ORDER_CONTRACT_REF, orderInput)
//                output(ORDER_CONTRACT_REF, orderOutput)
//                output(ORDER_CONTRACT_REF, orderOutput)
//                command(listOf(ALICE.publicKey, BOB.publicKey), OrderContract.Commands.Buy())
//                timeWindow(TimeWindow.between(Instant.now(), expiry))
//                this `fails with` "A Buy transaction should only have one output state"
//            }
//
//            transaction {
//                input(ORDER_CONTRACT_REF, orderInput)
//                output(ORDER_CONTRACT_REF, orderOutput)
//                command(listOf(ALICE.publicKey, BOB.publicKey), OrderContract.Commands.Buy())
//                timeWindow(TimeWindow.between(Instant.now(), expiry))
//                this.verifies()
//            }
//        }
//    }
//
//    @Ignore
//    fun mustBeTimestamped() {
//        val expiry = Instant.now().plusSeconds(3600)
//        val stock = Stock("description", "code", ALICE.party)
//        val orderInput = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiry, ALICE.party, null
//        )
//        val orderOutput = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiry, ALICE.party, BOB.party
//        )
//        ledgerServices.ledger {
//            transaction {
//                input(ORDER_CONTRACT_REF, orderInput)
//                output(ORDER_CONTRACT_REF, orderOutput)
//                command(listOf(ALICE.publicKey, BOB.publicKey), OrderContract.Commands.Buy())
//                this `fails with` "Sell Order must be timestamped"
//            }
//
//            transaction {
//                input(ORDER_CONTRACT_REF, orderInput)
//                output(ORDER_CONTRACT_REF, orderOutput)
//                command(listOf(ALICE.publicKey, BOB.publicKey), OrderContract.Commands.Buy())
//                timeWindow(TimeWindow.between(Instant.now(), expiry))
//                this.verifies()
//            }
//        }
//    }
//
//    @Test
//    fun sellOrderMustNotBeExpired() {
//        val expiry = Instant.now().minusSeconds(3600)
//        val stock = Stock("description", "code", ALICE.party)
//        val orderInput = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiry, ALICE.party, null
//        )
//        val orderOutput = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiry, ALICE.party, BOB.party
//        )
//
//        ledgerServices.ledger {
//            transaction {
//                input(ORDER_CONTRACT_REF, orderInput)
//                output(ORDER_CONTRACT_REF, orderOutput)
//                timeWindow(TimeWindow.fromOnly(Instant.now()))
//                command(listOf(ALICE.publicKey, BOB.publicKey), OrderContract.Commands.Buy())
//                this `fails with` "The order must not be expired"
//            }
//        }
//    }
//
//    @Ignore
//    fun inputStateTypeMustBeOrder() {
//        val expiry = Instant.now().plusSeconds(3600)
//        val stock = Stock("description", "code", ALICE.party)
//        val orderOutput = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiry, ALICE.party, BOB.party
//        )
//
//        ledgerServices.ledger {
//            transaction {
//                input(DUMMY_CONTRACT_ID, DummyContract.DummyState())
//                output(ORDER_CONTRACT_REF, orderOutput)
//                timeWindow(TimeWindow.fromOnly(Instant.now()))
//                command(listOf(ALICE.publicKey, BOB.publicKey), OrderContract.Commands.Buy())
//                this `fails with` "The input state must be an Order"
//            }
//        }
//    }
//
//
//    @Ignore
//    fun outputStateTypeMustBeOrder() {
//        val expiry = Instant.now().plusSeconds(3600)
//        val stock = Stock("description", "code", ALICE.party)
//        val orderInput = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiry, ALICE.party, null
//        )
//        val orderOutput = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiry, ALICE.party, BOB.party
//        )
//        ledgerServices.ledger {
//            transaction {
//                input(ORDER_CONTRACT_REF, orderInput)
//                output(DUMMY_CONTRACT_ID, DummyContract.DummyState())
//                timeWindow(TimeWindow.fromOnly(Instant.now()))
//                command(listOf(ALICE.publicKey, BOB.publicKey), OrderContract.Commands.Buy())
//                this `fails with` "The output state must be an Order"
//            }
//
//            transaction {
//                input(ORDER_CONTRACT_REF, orderInput)
//                output(ORDER_CONTRACT_REF, orderOutput)
//                timeWindow(TimeWindow.fromOnly(Instant.now()))
//                command(listOf(ALICE.publicKey, BOB.publicKey), OrderContract.Commands.Buy())
//                this.verifies()
//            }
//        }
//    }
//
//    @Test
//    fun buyerMustChange() {
//        val expiry = Instant.now().plusSeconds(3600)
//        val stock = Stock("description", "code", ALICE.party)
//        val orderInput = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiry, ALICE.party, null
//        )
//        var orderOutput = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiry, ALICE.party, BOB.party
//        )
//        ledgerServices.ledger {
//            transaction {
//                input(ORDER_CONTRACT_REF, orderInput)
//                output(ORDER_CONTRACT_REF, orderOutput)
//                timeWindow(TimeWindow.fromOnly(Instant.now()))
//                command(listOf(ALICE.publicKey, BOB.publicKey), OrderContract.Commands.Buy())
//                this `fails with` "Only the 'buyer' may change"
//            }
//
//            orderOutput = Order(
//                UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//                Direction.SELL, expiry, ALICE.party, BOB.party
//            )
//            orderOutput = Order(
//                UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//                Direction.SELL, expiry, ALICE.party, BOB.party
//            )
//            transaction {
//                input(ORDER_CONTRACT_REF, orderInput)
//                output(ORDER_CONTRACT_REF, orderOutput)
//                timeWindow(TimeWindow.fromOnly(Instant.now()))
//                command(listOf(ALICE.publicKey, BOB.publicKey), OrderContract.Commands.Buy())
//                this `fails with` "Only the 'buyer' may change"
//            }
//        }
//    }
//
//    @Test
//    fun buyerMustNotBeNull() {
//        val expiry = Instant.now().plusSeconds(3600)
//        val stock = Stock("description", "code", ALICE.party)
//        val orderInput = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiry, ALICE.party, null
//        )
//        val orderOutput = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiry, ALICE.party, null
//        )
//
//        ledgerServices.ledger {
//            transaction {
//                input(ORDER_CONTRACT_REF, orderInput)
//                output(ORDER_CONTRACT_REF, orderOutput)
//                timeWindow(TimeWindow.fromOnly(Instant.now()))
//                command(listOf(ALICE.publicKey, BOB.publicKey), OrderContract.Commands.Buy())
//                this `fails with` "The buyer cannot be 'null'"
//            }
//        }
//    }
//}