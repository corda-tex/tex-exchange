package com.ccc.flow

import com.ccc.state.Order
import com.ccc.state.Stock
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.utilities.getOrThrow
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
import kotlin.test.assertFailsWith

class OrderListFlowTest {
    companion object {
        val ONE_POUND = Amount.fromDecimal(BigDecimal(1), GBP)
        val ONE_DAY = Instant.now().plus(Duration.ofDays(1))
    }

    private lateinit var network: MockNetwork
    private lateinit var sellerNode: StartedMockNode
    private lateinit var buyerNode: StartedMockNode
    private lateinit var seller: Party
    private lateinit var buyer: Party

    @Before
    fun setup() {
        network = MockNetwork(
            MockNetworkParameters(
                cordappsForAllNodes = listOf(
                    TestCordapp.findCordapp("com.ccc.contract"),
                    TestCordapp.findCordapp("com.ccc.flow"))))

        sellerNode = network.createNode(CordaX500Name("sellerNode", "", "GB"))
        buyerNode = network.createNode(CordaX500Name("buyerNode", "", "GB"))
        seller = sellerNode.info.singleIdentity()
        buyer = buyerNode.info.singleIdentity()
    }

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `OrderListFlow will not find not existent stock`() {
        val stock = Stock("stock not in vault", "code", seller, 1)
        val flow = OrderListFlow(stock.linearId, ONE_POUND, stock.count, ONE_DAY)
        assertFailsWith<StockNotFoundException> {
            sellerNode.startFlow(flow).getOrThrow()
        }
    }

    @Test
    fun `OrderListFlow publishes and broadcasts to counter parties`() {
        // Issue stock and assert it exists in seller's vault.
        val issuedStockId = sellerNode.startFlow(SelfIssueStockFlow("IBM ", "IBM", 1)).get()
        val stock = sellerNode.services.vaultService.queryBy(Stock::class.java).states[0].state.data
        assert(issuedStockId == stock.linearId)

        val orderListFlow = OrderListFlow(stock.linearId, ONE_POUND, stock.count, ONE_DAY)
        val future = sellerNode.startFlow(orderListFlow)
        network.runNetwork()
        future.getOrThrow() // wait until orderListFlow is completed.
        val order = buyerNode.services.vaultService.queryBy(Order::class.java)
        assert(order.states.isNotEmpty())
    }

}