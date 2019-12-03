package com.ccc.flow


import com.ccc.state.Order
import com.ccc.state.Stock
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.utilities.getOrThrow
import net.corda.finance.POUNDS
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.collections.HashMap
import java.time.Instant
import org.assertj.core.api.Assertions.*


class OrderSettleFlowTests {

    /**
     * Create Mock Network
     */
    private val network = MockNetwork(
        MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.ccc.contract"),
            TestCordapp.findCordapp("com.ccc.flow")
        ))
    )
    private val dealerNodeOne = network.createNode(CordaX500Name("dealerNodeOne", "", "GB"))
    private val dealerNodeTwo = network.createNode(CordaX500Name("dealerNodeTwo", "", "GB"))
    private val playerNodeMap = HashMap<Party, StartedMockNode>()

    init {
        playerNodeMap[dealerNodeOne.info.legalIdentities.first()] = dealerNodeOne
        playerNodeMap[dealerNodeTwo.info.legalIdentities.first()] = dealerNodeTwo
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `Order Settle Flow should transfer the ownership of the Stock to the counterparty`() {
        val selfIssueStockFlow = SelfIssueStockFlow("IBM", "IBM", 10)
        val selfIssuefuture = dealerNodeOne.startFlow(selfIssueStockFlow)
        //network.runNetwork()
        val stockID = selfIssuefuture.toCompletableFuture().get()

        val businessId = "Order123"
        val orderListFlow = OrderListFlow(businessId, stockID, 10.POUNDS, 10, Instant.now().plusSeconds(100))
        val orderListfuture = dealerNodeOne.startFlow(orderListFlow)
        network.runNetwork()
        orderListfuture.toCompletableFuture().get()
        val order = dealerNodeOne.services.vaultService.queryBy(Order::class.java)
        val orderID = order.states.first().state.data.linearId
        assertThat(orderID).isInstanceOf(UniqueIdentifier::class.java)

        val orderBuyFlow = OrderBuyFlow(orderID, 10.POUNDS)
        val orderBuyFlowFuture = dealerNodeTwo.startFlow(orderBuyFlow)
        network.runNetwork()
        orderBuyFlowFuture.toCompletableFuture().get()
        val orderBoughtPage = dealerNodeTwo.services.vaultService.queryBy(Order::class.java)
        val orderBought = orderBoughtPage.states.first().state.data
        assertThat(orderBought.buyer).isEqualTo(dealerNodeTwo.info.legalIdentities.first())

        val orderSettleFlow = OrderSettleFlow(orderID, 10)
        val orderSettleFlowFuture = dealerNodeOne.startFlow(orderSettleFlow)
        network.runNetwork()
        orderSettleFlowFuture.toCompletableFuture().get()
        val stockTransferredPage = dealerNodeTwo.services.vaultService.queryBy(Stock::class.java)
        val stockTransferred = stockTransferredPage.states.first().state.data
        assertThat(stockTransferred.owner).isEqualTo(dealerNodeTwo.info.legalIdentities.first())
    }
}