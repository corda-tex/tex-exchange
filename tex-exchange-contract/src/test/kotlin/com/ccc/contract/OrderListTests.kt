//package com.ccc.contract
//
//import com.ccc.ALICE
//import com.ccc.BOB
//import com.ccc.contract.OrderContract.Companion.ORDER_CONTRACT_REF
//import com.ccc.contract.StockContract.Companion.STOCK_CONTRACT_REF
//import com.ccc.state.Direction
//import com.ccc.state.Order
//import com.ccc.state.Stock
//import net.corda.core.contracts.TimeWindow
//import net.corda.core.contracts.UniqueIdentifier
//import net.corda.finance.POUNDS
//import net.corda.testing.node.MockServices
//import net.corda.testing.node.ledger
//import org.junit.Test
//import java.time.Instant
//
//class OrderListTests {
//
//    var ledgerServices = MockServices(listOf("com.ccc"))
//
//    @Test
//    fun mustHandleListCommands() {
//        val expiryDateTime = Instant.now().plusSeconds(3600)
//        val stock = Stock("description", "code", ALICE.party, listed = false)
//        val order = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiryDateTime, ALICE.party, null
//        )
//
//        ledgerServices.ledger {
//            transaction {
//                input(STOCK_CONTRACT_REF, stock)
//                output(ORDER_CONTRACT_REF, order)
//                output(STOCK_CONTRACT_REF, stock.list())
//                command(ALICE.publicKey, StockContract.Commands.List())
//                timeWindow(TimeWindow.between(Instant.now(), expiryDateTime))
//                this `fails with` "Required com.ccc.contract.OrderContract.Commands command"
//            }
//
//            transaction {
//                input(STOCK_CONTRACT_REF, stock)
//                output(ORDER_CONTRACT_REF, order)
//                output(STOCK_CONTRACT_REF, stock.list())
//                command(ALICE.publicKey, OrderContract.Commands.List())
//                timeWindow(TimeWindow.between(Instant.now(), expiryDateTime))
//                this `fails with` "Required com.ccc.contract.StockContract.Commands command"
//            }
//
//            transaction {
//                input(STOCK_CONTRACT_REF, stock)
//                output(ORDER_CONTRACT_REF, order)
//                output(STOCK_CONTRACT_REF, stock.list())
//                timeWindow(TimeWindow.between(Instant.now(), expiryDateTime))
//                command(ALICE.publicKey, OrderContract.Commands.List())
//                command(ALICE.publicKey, StockContract.Commands.List())
//                this.verifies()
//            }
//        }
//    }
//
//    @Test
//    fun mustConsumeOnlyOneInput() {
//        val expiryDateTime = Instant.now().plusSeconds(3600)
//        val stock = Stock("description", "code", ALICE.party, listed = false)
//        val order = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiryDateTime, ALICE.party, null
//        )
//
//        ledgerServices.ledger {
//            transaction {
//                command(ALICE.publicKey, OrderContract.Commands.List())
//                command(ALICE.publicKey, StockContract.Commands.List())
//                input(STOCK_CONTRACT_REF, stock)
//                input(STOCK_CONTRACT_REF, Stock("description", "code", ALICE.party, listed = false))
//                output(ORDER_CONTRACT_REF, order)
//                output(STOCK_CONTRACT_REF, stock.list())
//                timeWindow(TimeWindow.between(Instant.now(), expiryDateTime))
//
//                this `fails with` "There must be only one Stock input"
//            }
//
//            transaction {
//                output(ORDER_CONTRACT_REF, order)
//                output(STOCK_CONTRACT_REF, stock.list())
//                timeWindow(TimeWindow.between(Instant.now(), expiryDateTime))
//                command(ALICE.publicKey, OrderContract.Commands.List())
//                command(ALICE.publicKey, StockContract.Commands.List())
//                this `fails with` "Only one input should be consumed when opening/listing a Sell order"
//            }
//        }
//    }
//
//    @Test
//    fun inputStateMustBeOfCorrectType() {
//        val expiryDateTime = Instant.now().plusSeconds(3600)
//        val stock = Stock("description", "code", ALICE.party, listed = false)
//        val order = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiryDateTime, ALICE.party, null
//        )
//
//        ledgerServices.ledger {
//            transaction {
//                input(ORDER_CONTRACT_REF, order)
//                output(ORDER_CONTRACT_REF, order)
//                output(STOCK_CONTRACT_REF, stock.list())
//                timeWindow(TimeWindow.between(Instant.now(), expiryDateTime))
//                command(ALICE.publicKey, OrderContract.Commands.List())
//                command(ALICE.publicKey, StockContract.Commands.List())
//                this `fails with` "The input state type should be Stock"
//            }
//        }
//    }
//
//    @Test
//    fun mustProduceExactlyTwoOutputs() {
//        val expiryDateTime = Instant.now().plusSeconds(3600)
//        val stock = Stock("description", "code", ALICE.party, listed = false)
//        val order = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiryDateTime, ALICE.party, null
//        )
//
//        ledgerServices.ledger {
//            transaction {
//                input(STOCK_CONTRACT_REF, stock)
//                output(ORDER_CONTRACT_REF, stock.list())
//                timeWindow(TimeWindow.between(Instant.now(), expiryDateTime))
//                command(ALICE.publicKey, OrderContract.Commands.List())
//                command(ALICE.publicKey, StockContract.Commands.List())
//                this `fails with` "Only two output states should be created when opening a Sell Order"
//            }
//
//            transaction {
//                input(STOCK_CONTRACT_REF, stock)
//                timeWindow(TimeWindow.between(Instant.now(), expiryDateTime))
//                command(ALICE.publicKey, OrderContract.Commands.List())
//                command(ALICE.publicKey, StockContract.Commands.List())
//                this `fails with` "There must be only one Stock output"
//            }
//
//            transaction {
//                input(STOCK_CONTRACT_REF, stock)
//                output(STOCK_CONTRACT_REF, stock.list())
//                output(ORDER_CONTRACT_REF, order)
//                output(STOCK_CONTRACT_REF, stock.list())
//                timeWindow(TimeWindow.between(Instant.now(), expiryDateTime))
//                command(ALICE.publicKey, OrderContract.Commands.List())
//                command(ALICE.publicKey, StockContract.Commands.List())
//                this `fails with` "There must be only one Stock output"
//            }
//        }
//    }
//
//    @Test
//    fun listingPriceMustBeGreaterThanZero() {
//        val expiryDateTime = Instant.now().plusSeconds(3600)
//        val stock = Stock("description", "code", ALICE.party, listed = false)
//        val order = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 0.POUNDS, 10,
//            Direction.SELL, expiryDateTime, ALICE.party, null
//        )
//
//        ledgerServices.ledger {
//            transaction {
//                input(ORDER_CONTRACT_REF, stock)
//                output(ORDER_CONTRACT_REF, order)
//                output(STOCK_CONTRACT_REF, stock.list())
//                timeWindow(TimeWindow.between(Instant.now(), expiryDateTime))
//                command(ALICE.publicKey, OrderContract.Commands.List())
//                command(ALICE.publicKey, StockContract.Commands.List())
//                this `fails with` "A newly issued Sell Order must have a starting price greater than zero"
//            }
//        }
//    }
//
//    @Test
//    fun orderListingsMustBeTimestamped() {
//        val expiry = Instant.now().plusSeconds(3600)
//        val stock = Stock("description", "code", ALICE.party, listed = false)
//        val order = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiry, ALICE.party, null
//        )
//
//        ledgerServices.ledger {
//            transaction {
//                input(ORDER_CONTRACT_REF, stock)
//                output(ORDER_CONTRACT_REF, order)
//                output(STOCK_CONTRACT_REF, stock.list())
//                command(ALICE.publicKey, OrderContract.Commands.List())
//                command(ALICE.publicKey, StockContract.Commands.List())
//                this `fails with` "Sell Order openings or listings must be timestamped"
//            }
//        }
//    }
//
//    @Test
//    fun expiryDateTimeMustBeInTheFuture() {
//        val expiry = Instant.now().minusSeconds(3600)
//        val stock = Stock("description", "code", ALICE.party, listed = false)
//        val order = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiry, ALICE.party, null
//        )
//
//        ledgerServices.ledger {
//            transaction {
//                input(STOCK_CONTRACT_REF, stock)
//                output(ORDER_CONTRACT_REF, order)
//                output(STOCK_CONTRACT_REF, stock.list())
//                timeWindow(TimeWindow.fromOnly(Instant.now()))
//                command(ALICE.publicKey, OrderContract.Commands.List())
//                command(ALICE.publicKey, StockContract.Commands.List())
//                this `fails with` "The expiry date cannot be in the past"
//            }
//        }
//    }
//
//    @Test
//    fun onlyTheSellerNeedsToSignTx() {
//        val expiry = Instant.now().plusSeconds(3600)
//        val stock = Stock("description", "code", ALICE.party, listed = false)
//        val order = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiry, ALICE.party, null
//        )
//
//        ledgerServices.ledger {
//            transaction {
//                input(STOCK_CONTRACT_REF, stock)
//                output(ORDER_CONTRACT_REF, order)
//                output(STOCK_CONTRACT_REF, stock.list())
//                timeWindow(TimeWindow.between(Instant.now(), expiry))
//                command(listOf(ALICE.publicKey, BOB.publicKey), OrderContract.Commands.List())
//                command(ALICE.publicKey, StockContract.Commands.List())
//                this `fails with` "Only the seller needs to sign the transaction"
//            }
//
//            transaction {
//                input(STOCK_CONTRACT_REF, stock)
//                output(ORDER_CONTRACT_REF, order)
//                output(STOCK_CONTRACT_REF, stock.list())
//                timeWindow(TimeWindow.between(Instant.now(), expiry))
//                command(ALICE.publicKey, OrderContract.Commands.List())
//                command(listOf(ALICE.publicKey, BOB.publicKey), StockContract.Commands.List())
//                this `fails with` "Only the owner needs to sign the transaction"
//            }
//        }
//    }
//
//    @Test
//    fun orderMustHaveNoBuyer() {
//        val expiry = Instant.now().plusSeconds(3600)
//        val stock = Stock("description", "code", ALICE.party, listed = false)
//        val order = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiry, ALICE.party, BOB.party
//        )
//
//        ledgerServices.ledger {
//            transaction {
//                input(STOCK_CONTRACT_REF, stock)
//                output(ORDER_CONTRACT_REF, order)
//                output(STOCK_CONTRACT_REF, stock.list())
//                timeWindow(TimeWindow.between(Instant.now(), expiry))
//                command(ALICE.publicKey, OrderContract.Commands.List())
//                command(ALICE.publicKey, StockContract.Commands.List())
//                this `fails with` "The Sell Order must have no buyer when listed"
//            }
//        }
//    }
//
//    @Test
//    fun onlyOwnerCanListStock() {
//        val expiry = Instant.now().plusSeconds(3600)
//        val stock = Stock("description", "code", ALICE.party, listed = false)
//        val order = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiry, BOB.party, null
//        )
//
//        ledgerServices.ledger {
//            transaction {
//                input(STOCK_CONTRACT_REF, stock)
//                output(ORDER_CONTRACT_REF, order)
//                output(STOCK_CONTRACT_REF, stock.list())
//                timeWindow(TimeWindow.between(Instant.now(), expiry))
//                command(BOB.publicKey, OrderContract.Commands.List())
//                command(ALICE.publicKey, StockContract.Commands.List())
//                this `fails with` "Only the owner of the Stock can list it in a Sell Order"
//            }
//        }
//    }
//
//    @Test
//    fun stockNotAlreadyListed() {
//        val expiry = Instant.now().plusSeconds(3600)
//        val stock = Stock("description", "code", ALICE.party, listed = true)
//        val order = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiry, BOB.party, null
//        )
//
//        ledgerServices.ledger {
//            transaction {
//                input(STOCK_CONTRACT_REF, stock)
//                output(ORDER_CONTRACT_REF, order)
//                output(STOCK_CONTRACT_REF, stock.list())
//                timeWindow(TimeWindow.between(Instant.now(), expiry))
//                command(ALICE.publicKey, OrderContract.Commands.List())
//                command(BOB.publicKey, StockContract.Commands.List())
//                this `fails with` "The 'listed' stock must be false in the input state"
//            }
//        }
//    }
//
//    @Test
//    fun outputStockMustBeListed() {
//        val expiry = Instant.now().plusSeconds(3600)
//        val stock = Stock("description", "code", ALICE.party, listed = false)
//        val order = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiry, BOB.party, null
//        )
//
//        ledgerServices.ledger {
//            transaction {
//                input(STOCK_CONTRACT_REF, stock)
//                output(ORDER_CONTRACT_REF, order)
//                output(STOCK_CONTRACT_REF, stock)
//                timeWindow(TimeWindow.between(Instant.now(), expiry))
//                command(ALICE.publicKey, OrderContract.Commands.List())
//                command(BOB.publicKey, StockContract.Commands.List())
//                this `fails with` "The 'listed' stock must be true in the output state"
//            }
//        }
//    }
//
//    @Test
//    fun onlyListedPropertyShouldChange() {
//        val expiry = Instant.now().plusSeconds(3600)
//        val stock = Stock("description", "code", ALICE.party, listed = false)
//        val order = Order(
//            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
//            Direction.SELL, expiry, BOB.party, null
//        )
//
//        ledgerServices.ledger {
//            transaction {
//                input(STOCK_CONTRACT_REF, stock)
//                output(ORDER_CONTRACT_REF, order)
//                output(STOCK_CONTRACT_REF, stock.copy(listed = true, description = "Microsoft Stock for £200"))
//                timeWindow(TimeWindow.between(Instant.now(), expiry))
//                command(ALICE.publicKey, OrderContract.Commands.List())
//                command(BOB.publicKey, StockContract.Commands.List())
//                this `fails with` "Only the 'listed' stock can change"
//            }
//        }
//    }
//
//}