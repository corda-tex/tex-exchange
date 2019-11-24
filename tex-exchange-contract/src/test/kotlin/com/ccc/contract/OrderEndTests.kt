package com.ccc.contract


import com.ccc.ALICE
import com.ccc.BOB
import com.ccc.CHARLIE
import com.ccc.contract.OrderContract.Companion.ORDER_CONTRACT_REF
import com.ccc.contract.StockContract.Companion.STOCK_CONTRACT_REF
import com.ccc.state.Direction
import com.ccc.state.Order
import com.ccc.state.Stock
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.finance.POUNDS
import net.corda.testing.node.ledger
import net.corda.testing.node.MockServices

import org.junit.Test

import java.time.Instant

class OrderEndTests {

    var ledgerServices = MockServices(listOf("com.ccc.contract"))
    val singleAmazonStock = Stock(UniqueIdentifier(), "Amazon AMZN 10 units for £10", "AMZN", ALICE.party, 1,true)

    @Test
    fun onlyOneOrderInput() {
        val expiry = Instant.now().plusSeconds(3600)
        val stock = singleAmazonStock
        val order = Order(
            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
            Direction.SELL, expiry, ALICE.party, BOB.party
        )

        ledgerServices.ledger {
            transaction {
                input(ORDER_CONTRACT_REF, order)
                input(ORDER_CONTRACT_REF, order)
                input(STOCK_CONTRACT_REF, stock)
                output(STOCK_CONTRACT_REF, stock.deList())
                command(listOf(ALICE.publicKey, BOB.publicKey), OrderContract.Commands.End())
                command(listOf(ALICE.publicKey, BOB.publicKey), StockContract.Commands.Delist())
                timeWindow(TimeWindow.fromOnly(Instant.now()))
                this `fails with` "There must be only one Order input"
            }

            transaction {
                input(ORDER_CONTRACT_REF, order)
                input(STOCK_CONTRACT_REF, stock)
                output(STOCK_CONTRACT_REF, stock.deList())
                timeWindow(TimeWindow.fromOnly(Instant.now()))
                command(listOf(ALICE.publicKey, BOB.publicKey), OrderContract.Commands.End())
                command(listOf(ALICE.publicKey, BOB.publicKey), StockContract.Commands.Delist())
                this.verifies()
            }
        }
    }

    @Test
    fun noOrderOutputs() {
        val expiry = Instant.now().plusSeconds(3600)
        val stock = singleAmazonStock
        val order = Order(
            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
            Direction.SELL, expiry, ALICE.party, BOB.party
        )
        ledgerServices.ledger {
            transaction {
                input(ORDER_CONTRACT_REF, order)
                input(STOCK_CONTRACT_REF, stock)
                output(ORDER_CONTRACT_REF, order)
                output(STOCK_CONTRACT_REF, stock.deList())
                command(listOf(ALICE.publicKey, BOB.publicKey), OrderContract.Commands.End())
                command(listOf(ALICE.publicKey, BOB.publicKey), StockContract.Commands.Delist())
                timeWindow(TimeWindow.fromOnly(Instant.now()))
                this `fails with` "There must not be any Order outputs"
            }
        }
    }

    @Test
    fun mustBeTimestamped() {
        val expiry = Instant.now().plusSeconds(3600)
        val stock = singleAmazonStock
        val order = Order(
            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
            Direction.SELL, expiry, ALICE.party, BOB.party
        )
        ledgerServices.ledger {
            transaction {
                input(ORDER_CONTRACT_REF, order)
                input(STOCK_CONTRACT_REF, stock)
                output(STOCK_CONTRACT_REF, stock.deList())
                command(listOf(ALICE.publicKey, BOB.publicKey), OrderContract.Commands.End())
                command(listOf(ALICE.publicKey, BOB.publicKey), StockContract.Commands.Delist())
                this `fails with` "Transaction must be timestamped"
            }
        }
    }

    @Test
    fun doesNotHaveToBeExpired() {
        val expiry = Instant.now().minusSeconds(3600)
        val stock = singleAmazonStock
        val order = Order(
            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
            Direction.SELL, expiry, ALICE.party, BOB.party
        )
        ledgerServices.ledger {
            transaction {
                input(ORDER_CONTRACT_REF, order)
                input(STOCK_CONTRACT_REF, stock)
                output(STOCK_CONTRACT_REF, stock.deList())
                command(listOf(ALICE.publicKey, BOB.publicKey), OrderContract.Commands.End())
                command(listOf(ALICE.publicKey, BOB.publicKey), StockContract.Commands.Delist())
                timeWindow(TimeWindow.fromOnly(Instant.now()))
                this.verifies()
            }
        }
    }

    @Test
    fun sellerAndBuyerMustSign() {
        val expiry = Instant.now().plusSeconds(3600)
        val stock = singleAmazonStock
        val order = Order(
            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
            Direction.SELL, expiry, ALICE.party, BOB.party
        )
        ledgerServices.ledger {
            transaction {
                input(ORDER_CONTRACT_REF, order)
                input(STOCK_CONTRACT_REF, stock)
                output(STOCK_CONTRACT_REF, stock.deList())
                timeWindow(TimeWindow.fromOnly(Instant.now()))
                command(ALICE.publicKey, OrderContract.Commands.End())
                command(listOf(ALICE.publicKey, BOB.publicKey), StockContract.Commands.Delist())
                this `fails with` "Both seller and buyer only must sign the Order to end transaction"
            }

            transaction {
                input(ORDER_CONTRACT_REF, order)
                input(STOCK_CONTRACT_REF, stock)
                output(STOCK_CONTRACT_REF, stock.deList())
                timeWindow(TimeWindow.fromOnly(Instant.now()))
                command(listOf(ALICE.publicKey, BOB.publicKey, CHARLIE.publicKey), OrderContract.Commands.End())
                command(listOf(ALICE.publicKey, BOB.publicKey), StockContract.Commands.Delist())
                this `fails with` "Both seller and buyer only must sign the Order to end transaction"
            }
        }
    }

    @Test
    fun onlyOneStock() {
        val expiry = Instant.now().plusSeconds(3600)
        val stock = singleAmazonStock
        val order = Order(
            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
            Direction.SELL, expiry, ALICE.party, BOB.party
        )
        ledgerServices.ledger {
            transaction {
                input(ORDER_CONTRACT_REF, order)
                input(STOCK_CONTRACT_REF, stock)
                input(STOCK_CONTRACT_REF, stock)
                output(STOCK_CONTRACT_REF, stock.deList())
                timeWindow(TimeWindow.fromOnly(Instant.now()))
                command(listOf(ALICE.publicKey, BOB.publicKey), OrderContract.Commands.End())
                command(listOf(ALICE.publicKey, BOB.publicKey), StockContract.Commands.Delist())
                this `fails with` "There must be only one Stock input"
            }

            transaction {
                input(ORDER_CONTRACT_REF, order)
                input(STOCK_CONTRACT_REF, stock)
                output(STOCK_CONTRACT_REF, stock.deList())
                output(STOCK_CONTRACT_REF, stock.deList())
                timeWindow(TimeWindow.fromOnly(Instant.now()))
                command(listOf(ALICE.publicKey, BOB.publicKey), OrderContract.Commands.End())
                command(listOf(ALICE.publicKey, BOB.publicKey), StockContract.Commands.Delist())
                this `fails with` "There must be only one Stock output"
            }
        }
    }

    @Test
    fun ifNoBuyerOnlyOwnerMustSign() {
        val expiry = Instant.now().plusSeconds(3600)
        val stock = singleAmazonStock
        val order = Order(
            UniqueIdentifier(), stock.linearId, stock.description, 10.POUNDS, 10,
            Direction.SELL, expiry, ALICE.party, null
        )
        ledgerServices.ledger {
            transaction {
                input(ORDER_CONTRACT_REF, order)
                input(STOCK_CONTRACT_REF, stock)
                output(STOCK_CONTRACT_REF, stock.deList())
                timeWindow(TimeWindow.fromOnly(Instant.now()))
                command(ALICE.publicKey, OrderContract.Commands.End())
                command(listOf(ALICE.publicKey, BOB.publicKey), StockContract.Commands.Delist())
                this `fails with` "Only the owner must sign a de-list transaction"
            }
        }
    }}
