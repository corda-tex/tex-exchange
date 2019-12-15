package com.ccc.flow

import com.ccc.state.Order
import com.ccc.state.Stock
import net.corda.core.contracts.Amount
import net.corda.core.flows.NotaryException
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.finance.GBP
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

open class OrderBuyFlowTests {
    companion object {
        val ONE_POUND = Amount.fromDecimal(BigDecimal(1), GBP)
        val ONE_DAY = Instant.now().plus(Duration.ofDays(1))
    }

    protected lateinit var network: MockNetwork
    protected lateinit var sellerNode: StartedMockNode
    protected lateinit var buyerNode: StartedMockNode
    protected lateinit var seller: Party
    protected lateinit var buyer: Party

    @Before
    fun setup() {
        network = MockNetwork(
            MockNetworkParameters(
                cordappsForAllNodes = listOf(
                    TestCordapp.findCordapp("com.ccc.contract"),
                    TestCordapp.findCordapp("com.ccc.flow")))
        )

        sellerNode = network.createNode(CordaX500Name("sellerNode", "", "GB"))
        buyerNode = network.createNode(CordaX500Name("buyerNode", "", "GB"))
        seller = sellerNode.info.singleIdentity()
        buyer = buyerNode.info.singleIdentity()
    }

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `OrderBuyFlow getSortedOrdersForStock works`() {
        val stockId = TestUtils.issueStockToNode(sellerNode, null, "potatoes", 10)

        val orderFlow1 = OrderListFlow(stockId, OrderListFlowTests.TWO_POUNDS, 5, OrderListFlowTests.ONE_DAY)
        val orderFlow2 = OrderListFlow(stockId, OrderListFlowTests.ONE_POUND, 5, OrderListFlowTests.ONE_DAY)
        val future1 = sellerNode.startFlow(orderFlow1)
        network.runNetwork()
        future1.get()
        val future2 = sellerNode.startFlow(orderFlow2)
        network.runNetwork()
        future2.get()

        val peerOrderBook = buyerNode.services.vaultService.queryBy(Order::class.java).states
        assertEquals(2, peerOrderBook.size)
        val sortedOrderStates = OrderBuyFlow(stockId, 0)
            .getSortedOrdersForStock(buyerNode.services.vaultService, stockId)

        assert(sortedOrderStates[0].state.data.price == OrderListFlowTests.ONE_POUND)
        assert(sortedOrderStates[1].state.data.price == OrderListFlowTests.TWO_POUNDS)
    }

    @Test
    fun `OrderBuyFlow buys cheapest stock from order book`() {
        val stockId = TestUtils.issueStockToNode(sellerNode, null, "potatoes", 10)

        val orderFlow1 = OrderListFlow(stockId, OrderListFlowTests.TWO_POUNDS, 5, OrderListFlowTests.ONE_DAY)
        val orderFlow2 = OrderListFlow(stockId, OrderListFlowTests.ONE_POUND, 5, OrderListFlowTests.ONE_DAY)
        val future1 = sellerNode.startFlow(orderFlow1)
        network.runNetwork()
        future1.get()
        val future2 = sellerNode.startFlow(orderFlow2)
        network.runNetwork()
        future2.get()

        val buyFlow = OrderBuyFlow(stockId, 6)
        val future3 = buyerNode.startFlow(buyFlow)
        network.runNetwork()
        val boughtOrders = future3.get()

        assertEquals(boughtOrders.size,  2)
        assertEquals(boughtOrders[0].state,  Order.State.BOUGHT)
        assertEquals(boughtOrders[1].state,  Order.State.BOUGHT)
    }

    @Test
    fun `OrderBuyFlow buys from multiple sellers`() {
        val sellerNode2 = network.createNode(CordaX500Name("sellerNode2", "", "GB"))

        val stockId = TestUtils.issueStockToNode(sellerNode, null, "potatoes", 10)
        TestUtils.issueStockToNode(sellerNode2, stockId, "potatoes", 10)

        val orderFlow1 = OrderListFlow(stockId, OrderListFlowTests.TWO_POUNDS, 5, OrderListFlowTests.ONE_DAY)
        val future1 = sellerNode.startFlow(orderFlow1)
        network.runNetwork()
        future1.get()

        val orderFlow2 = OrderListFlow(stockId, OrderListFlowTests.ONE_POUND, 5, OrderListFlowTests.ONE_DAY)
        val future2 = sellerNode2.startFlow(orderFlow2)
        network.runNetwork()
        future2.get()

        val buyFlow = OrderBuyFlow(stockId, 10)
        val future3 = buyerNode.startFlow(buyFlow)
        network.runNetwork()
        val boughtOrders = future3.get()

        assertEquals(boughtOrders.size,  2)

        // havent settled stock yet -> should not have any
        assertEquals(0 , buyerNode.services.vaultService.queryBy(Stock::class.java).states.size)



//        val settleFlow = OrderSettleFlow()
//        val future4 = sellerNode.startFlow(settleFlow)
//        network.runNetwork()
//        val settledStocks = future4.get()
//        assertEquals(1, settledStocks.size)
    }



    @Test
    fun `OrderBuyFlow trying to buy an already bought sellOrder fails`() {
        val buyerNode2 = network.createNode(CordaX500Name("buyerNode2", "", "GB"))

        val stockId = TestUtils.issueStockToNode(sellerNode, null, "potatoes", 10)
        val orderFlow = OrderListFlow(stockId, OrderListFlowTests.TWO_POUNDS, 5, OrderListFlowTests.ONE_DAY)
        val future = sellerNode.startFlow(orderFlow)
        network.runNetwork()
        future.get()

        val buyFlow = OrderBuyFlow(stockId, 5)
        val future1 = buyerNode.startFlow(buyFlow) // buyerNode buys stock.
        network.runNetwork()
        val boughtOrders = future1.get()
        assertEquals(boughtOrders.size,  1)

        // Buyer2 will try to get the same stock
        val buyFlow2 = OrderBuyFlow(stockId, 5)
        val future2 = buyerNode2.startFlow(buyFlow2)
        network.runNetwork()
        var boughtOrders2: List<Order>? = null
        val e = assertFailsWith<ExecutionException> {
            boughtOrders2 = future2.get()
        }

        assertNull(boughtOrders2)

        assert(e.cause != null
                && e.cause is NotaryException
                && e.message!!.contains("One or more input states or referenced states have already been used as input states in other transactions."))

    }

}