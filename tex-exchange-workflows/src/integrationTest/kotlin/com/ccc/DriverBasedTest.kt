//package com.ccc
//
//import com.ccc.flow.OrderBuyFlow
//import com.ccc.flow.OrderListFlow
//import com.ccc.flow.OrderSettleFlow
//import com.ccc.flow.SelfIssue
//import com.ccc.flow.SelfIssue.SelfIssueStockFlow
//import com.ccc.state.Order
//import com.ccc.state.Stock
//import net.corda.core.concurrent.CordaFuture
//import net.corda.core.crypto.SecureHash
//import net.corda.core.identity.CordaX500Name
//import net.corda.core.internal.concurrent.doneFuture
//import net.corda.core.internal.concurrent.transpose
//import net.corda.core.messaging.CordaRPCOps
//import net.corda.core.messaging.startFlow
//import net.corda.core.toFuture
//import net.corda.core.transactions.SignedTransaction
//import net.corda.core.utilities.contextLogger
//import net.corda.core.utilities.getOrThrow
//import net.corda.finance.POUNDS
//import net.corda.testing.common.internal.testNetworkParameters
//import net.corda.testing.core.TestIdentity
//import net.corda.testing.driver.*
//import net.corda.testing.node.TestCordapp
//import org.junit.Test
//import java.time.Instant
//import java.util.concurrent.Future
//import kotlin.test.assertEquals
//
//class DriverBasedTest {
//    companion object {
//        private val log = contextLogger()
//    }
//
//    private val brokerAParameters = NodeParameters(
//        providedName = CordaX500Name("BrokerA", "London", "GB"),
//        additionalCordapps = listOf(),
//        startInSameProcess = true
//    )
//
//    private val brokerBParameters = NodeParameters(
//        providedName = CordaX500Name("BrokerB", "London", "GB"),
//        additionalCordapps = listOf(),
//        startInSameProcess = true
//    )
//
//    private val nodeParams = listOf(brokerAParameters, brokerBParameters)
//
//    private val sellerBroker = TestIdentity(brokerAParameters.providedName!!)
//    private val buyerBroker = TestIdentity(brokerBParameters.providedName!!)
//
//    private val defaultCorDapps = listOf(
//        TestCordapp.findCordapp("com.ccc.flow"),
//        TestCordapp.findCordapp("com.ccc.contract")
//    )
//    private val driverParameters = DriverParameters(
//        startNodesInProcess = true,
//        cordappsForAllNodes = defaultCorDapps,
//        networkParameters = testNetworkParameters(notaries = emptyList(), minimumPlatformVersion = 4)
//    )
//
//    fun NodeHandle.legalIdentity() = nodeInfo.legalIdentities.single()
//
//
////    @Test
////    fun `MergeCashFlow test`() = withDriver {
////        val A = startNode(brokerBParameters)
////        val stockIdentity = A.rpc.startFlow(SelfIssue::SelfIssueStockFlow, "stockDescription", "code", 1).returnValue.getOrThrow()
////
////    }
//
//
//    //TODO: Is it possible to reduce the time taken to execute this test ?
//    @Test
//    fun `Issue Stock, Create Order and Broadcast of the Order should store the Order in the couterpartys vault`() {
//        driver(driverParameters) {
//            val (A, B) = nodeParams.map { params -> startNode(params) }.transpose().getOrThrow()
//            log.info("All nodes started up.")
//
//            log.info("Creating one Stock on node A.")
//            val stockIdentity = A.rpc.startFlow(::SelfIssueStockFlow, "stockDescription", "code", 1).returnValue.getOrThrow()
//
//            // Check that A recorded all the new accounts.
//            val aStock = A.rpc.vaultQuery(Stock::class.java).states.single().state.data
//            assertEquals(aStock.description, "stockDescription")
//            assertEquals(stockIdentity, aStock.linearId)
//
//            log.info("Create an Order and List it to parties")
//            val signedTransaction =
//                A.rpc.startFlow(::OrderListFlow, stockIdentity, 10.POUNDS, 10, Instant.now().plusSeconds(200))
//                    .returnValue.getOrThrow()
//            log.info("Test Complete")
//            Thread.sleep(3000)
//            val bOrder = B.rpc.vaultQuery(Order::class.java).states.single().state.data
//            assertEquals(bOrder.stockLinearId, stockIdentity)
//        }
//    }
//
//    @Test
//    fun `OrderBuyFlow should transfer the order to counterparty`() {
//        driver(driverParameters) {
//            val (A, B) = nodeParams.map { params -> startNode(params) }.transpose().getOrThrow()
//
//            // Creating stock on node A.
//            val stockId = A.rpc.startFlow(::SelfIssueStockFlow, "stockDescription", "code", 1).returnValue.getOrThrow()
//
//            // Check that A recorded the stock.
//            val aStock = A.rpc.vaultQuery(Stock::class.java).states.single().state.data
//            assertEquals(aStock.description, "stockDescription")
//            assertEquals(stockId, aStock.linearId)
//
//            // Create an order and list it to parties.
//            A.rpc.startFlow(::OrderListFlow, stockId, 10.POUNDS, 10, Instant.now().plusSeconds(2000000)).returnValue.getOrThrow()
//            Thread.sleep(3000)
//            val listedOrder = B.rpc.vaultQuery(Order::class.java).states.single().state.data
//
//            // B buys order.
//            B.rpc.startFlow(::OrderBuyFlow, listedOrder.linearId, 10.POUNDS).returnValue.getOrThrow()
//            Thread.sleep(3000)
//            val boughtOrder = B.rpc.vaultQuery(Order::class.java).states.single().state.data
//
//            assertEquals(boughtOrder.buyer, B.legalIdentity())
//        }
//    }
//
//    //TODO: TO solve this defect - The contract verification fails
//    @Test
//    fun `OrderSettleFlow should transfer the stock to counterparty`() {
//        driver(driverParameters) {
//            // Start nodes.
//            val (A, B) = nodeParams.map { params -> startNode(params) }.transpose().getOrThrow()
//
//            // A issues stock.
//            val stockIdentity = A.rpc.startFlow(::SelfIssueStockFlow, "stockDescription", "code", 1).returnValue.getOrThrow()
//
//            // Check stock is issued on A.
//            val aStock = A.rpc.vaultQuery(Stock::class.java).states.single().state.data
//            assertEquals(aStock.description, "stockDescription")
//            assertEquals(stockIdentity, aStock.linearId)
//
//            // Create an order and list it to parties.
//            A.rpc.startFlow(::OrderListFlow, stockIdentity, 10.POUNDS, 10, Instant.now().plusSeconds(200)).returnValue.getOrThrow()
//            Thread.sleep(3000)
//            val listedOrder = B.rpc.vaultQuery(Order::class.java).states.single().state.data
//
//            log.info("Counterparty Buys Order")
//            B.rpc.startFlow(::OrderBuyFlow, listedOrder.linearId, 10.POUNDS).returnValue.getOrThrow()
//            Thread.sleep(3000)
//            val boughtOrder = B.rpc.vaultQuery(Order::class.java).states.single().state.data
//
//            // Party settles order.
//            val signedTransactionSettleOrder =
//                A.rpc.startFlow(::OrderSettleFlow, listedOrder.linearId)
//                    .returnValue.getOrThrow()
//            Thread.sleep(3000)
//            val soldStock = B.rpc.vaultQuery(Stock::class.java).states.single().state.data
//            assertEquals(soldStock.owner, B.legalIdentity())
//
//        }
//    }
//
//    // Runs a test inside the Driver DSL, which provides useful functions for starting nodes, etc.
//    private fun withDriver(test: DriverDSL.() -> Unit) = driver(
//        DriverParameters(isDebug = true, startNodesInProcess = true)
//    ) { test() }
//
//    // Makes an RPC call to retrieve another node's name from the network map.
//    private fun NodeHandle.resolveName(name: CordaX500Name) = rpc.wellKnownPartyFromX500Name(name)!!.name
//
//    // Resolves a list of futures to a list of the promised values.
//    private fun <T> List<Future<T>>.waitForAll(): List<T> = map { it.getOrThrow() }
//
//    // Starts multiple nodes simultaneously, then waits for them all to be ready.
//    private fun DriverDSL.startNodes(vararg identities: TestIdentity) = identities
//        .map { startNode(providedName = it.name) }
//        .waitForAll()
//
//    fun CordaRPCOps.watchForTransaction(tx: SignedTransaction): CordaFuture<SignedTransaction> {
//        val (snapshot, feed) = internalVerifiedTransactionsFeed()
//        return if (tx in snapshot) {
//            doneFuture(tx)
//        } else {
//            feed.filter { it.id == tx.id }.toFuture()
//        }
//    }
//
//    fun CordaRPCOps.watchForTransaction(txId: SecureHash): CordaFuture<SignedTransaction> {
//        val (snapshot, feed) = internalVerifiedTransactionsFeed()
//        return if (txId in snapshot.map { it.id }) {
//            doneFuture(snapshot.single { txId == it.id })
//        } else {
//            feed.filter { it.id == txId }.toFuture()
//        }
//    }
//}