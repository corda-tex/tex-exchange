package com.ccc.newflow

import com.ccc.flow.*
import com.ccc.flow.CashTokenFlow.IssueCashTokenFlow
import com.ccc.flow.CashTokenFlow.MoveCashTokenFlow
import com.ccc.state.Order
import com.ccc.state.Stock
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.GBP
import net.corda.core.concurrent.CordaFuture
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.concurrent.doneFuture
import net.corda.core.internal.concurrent.transpose
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.toFuture
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.contextLogger
import net.corda.core.utilities.getOrThrow
import net.corda.finance.POUNDS
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.driver.*
import net.corda.testing.node.TestCordapp
import org.junit.Ignore
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.Future
import kotlin.test.assertEquals

class DriverBasedIssueCashTokenFlowTest {
    companion object {
        private val log = contextLogger()
    }

    private val brokerAParameters = NodeParameters(
        providedName = CordaX500Name("BrokerA", "London", "GB"),
        additionalCordapps = listOf()
    )

    private val brokerBParameters = NodeParameters(
        providedName = CordaX500Name("BrokerB", "London", "GB"),
        additionalCordapps = listOf()
    )

    private val bankOfEnglandParameters = NodeParameters(
        providedName = CordaX500Name("BankOfEngland", "London", "GB"),
        additionalCordapps = listOf()
    )

    private val nodeParams = listOf(brokerAParameters, brokerBParameters, bankOfEnglandParameters)

    private val sellerBroker = TestIdentity(brokerAParameters.providedName!!)
    private val buyerBroker = TestIdentity(brokerBParameters.providedName!!)
    private val bankOfEnglandBroker = TestIdentity(bankOfEnglandParameters.providedName!!)

    private val defaultCorDapps = listOf(
        TestCordapp.findCordapp("com.ccc.flow"),
        TestCordapp.findCordapp("com.ccc.contract"),
        TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts")
    )

    private val driverParameters = DriverParameters(
        isDebug = true,
        startNodesInProcess = true,
        cordappsForAllNodes = defaultCorDapps,
        networkParameters = testNetworkParameters(notaries = emptyList(), minimumPlatformVersion = 4)
    )

    fun NodeHandle.legalIdentity() = nodeInfo.legalIdentities.single()


    @Test
    fun `node test`() = withDriver {
        // Start a pair of nodes and wait for them both to be ready.
        val (partyAHandle, partyBHandle, partyBofEngland) = startNodes(sellerBroker, buyerBroker, bankOfEnglandBroker)

        // From each node, make an RPC call to retrieve another node's name from the network map, to verify that the
        // nodes have started and can communicate.

        // This is a very basic test: in practice tests would be starting flows, and verifying the states in the vault
        // and other important metrics to ensure that your CorDapp is working as intended.
        assertEquals(buyerBroker.name, partyAHandle.resolveName(buyerBroker.name))
        assertEquals(sellerBroker.name, partyBHandle.resolveName(sellerBroker.name))
        assertEquals(bankOfEnglandBroker.name, partyBofEngland.resolveName(bankOfEnglandBroker.name))
    }

    /*@Test
    @Ignore("Test hangs on notary")
    fun `Issue Cash and Move`() {
        //Given
        val bankOfEnglandCash = BigDecimal(1000.00)
        val dealerOneCash = BigDecimal(250.00)
        val issueCashTokenFlow = CashTokenFlow.IssueCashTokenFlow(bankOfEnglandCash)
        val moveCashTokenFlow = CashTokenFlow.MoveCashTokenFlow(dealerOneCash, partyNodeMap[dealerNodeOne]!!)
        //When
        bankOfEngland.startFlow(issueCashTokenFlow).getOrThrow()
        //    network.runNetwork()
        bankOfEngland.startFlow(moveCashTokenFlow).getOrThrow()
        network.runNetwork()
        //Then
        val cashToken = dealerNodeOne.services.vaultService.queryBy(FungibleToken::class.java).states[0].state.data
        val amountOfIssuedToken = dealerOneCash of GBP issuedBy partyNodeMap[bankOfEngland]!!
        assertEquals(cashToken.amount, amountOfIssuedToken)

    }*/


    @Test
    @Ignore("net.corda.core.flows.UnexpectedFlowEndException: O=BrokerA, L=London, C=GB has finished prematurely " +
            "and we're trying to send them the finalised transaction. Did they forget to call ReceiveFinalityFlow? " +
            "(com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens is not registered)")
    fun `Issue Stock, Create Order and Broadcast of the Order should store the Order in the couterpartys vault`() {
        driver(driverParameters) {
            val (A, B, BankOfEngland) = nodeParams.map { params -> startNode(params) }.transpose().getOrThrow()
            log.info("All nodes started up.")

            log.info("BankOfEngland is selfIssuing 1000.00.")
            val bankOfEnglandCash = BigDecimal(1000.00)
            val dealerOneCash = BigDecimal(250.00)
            val issueCashTokenFlow = CashTokenFlow.IssueCashTokenFlow(bankOfEnglandCash)
            BankOfEngland.rpc.startFlow(::IssueCashTokenFlow, bankOfEnglandCash).returnValue.getOrThrow()
            log.info("BankOfEngland is transfering to selfIssuing 1000.00.")
            BankOfEngland.rpc.startFlow(::MoveCashTokenFlow, dealerOneCash, A.nodeInfo.legalIdentities.first()).returnValue.getOrThrow()
            Thread.sleep(3000)
            /*val stockIdentity =
                A.rpc.startFlow(::SelfIssueStockFlow, "Google GOOGL 10 units", "GOOGL", 10).returnValue.getOrThrow()

            // Check that A recorded all the new accounts.
            val aStock = A.rpc.vaultQuery(Stock::class.java).states.single().state.data
            assertEquals(aStock.description, "Google GOOGL 10 units")
            assertEquals(stockIdentity, aStock.linearId)

            log.info("Create an Order and List it to parties")
            A.rpc.startFlow(::OrderListFlow, "Order1", stockIdentity, 10.POUNDS, 10, Instant.now().plusSeconds(200))
                    .returnValue.getOrThrow()
            log.info("Test Complete")
            Thread.sleep(3000)
            val bOrder = B.rpc.vaultQuery(Order::class.java).states.single().state.data
            assertEquals(bOrder.stockLinearId, stockIdentity)*/
        }
    }


    // Runs a test inside the Driver DSL, which provides useful functions for starting nodes, etc.
    private fun withDriver(test: DriverDSL.() -> Unit) = driver(
        DriverParameters(isDebug = true, startNodesInProcess = true)
    ) { test() }

    // Makes an RPC call to retrieve another node's name from the network map.
    private fun NodeHandle.resolveName(name: CordaX500Name) = rpc.wellKnownPartyFromX500Name(name)!!.name

    // Resolves a list of futures to a list of the promised values.
    private fun <T> List<Future<T>>.waitForAll(): List<T> = map { it.getOrThrow() }

    // Starts multiple nodes simultaneously, then waits for them all to be ready.
    private fun DriverDSL.startNodes(vararg identities: TestIdentity) = identities
        .map { startNode(providedName = it.name) }
        .waitForAll()

    fun CordaRPCOps.watchForTransaction(tx: SignedTransaction): CordaFuture<SignedTransaction> {
        val (snapshot, feed) = internalVerifiedTransactionsFeed()
        return if (tx in snapshot) {
            doneFuture(tx)
        } else {
            feed.filter { it.id == tx.id }.toFuture()
        }
    }

    fun CordaRPCOps.watchForTransaction(txId: SecureHash): CordaFuture<SignedTransaction> {
        val (snapshot, feed) = internalVerifiedTransactionsFeed()
        return if (txId in snapshot.map { it.id }) {
            doneFuture(snapshot.single { txId == it.id })
        } else {
            feed.filter { it.id == txId }.toFuture()
        }
    }
}