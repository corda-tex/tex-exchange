package com.ccc.flow

import com.ccc.state.Order
import org.junit.Test
import kotlin.test.assertEquals

class OrderSettleFlowTests: OrderBuyFlowTests() {

    @Test
    fun `OrderSettleFlow settles`() {
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

        val settleFlow = OrderSettleFlow()
        val future4 = sellerNode.startFlow(settleFlow)
        network.runNetwork()
        val settledStocks = future4.get()
        assertEquals(2, settledStocks.size)

        val settledOrders = sellerNode.services.vaultService.queryBy(Order::class.java)
            .states.filter { it.state.data.state == Order.State.SETTLED }

        assertEquals(settledOrders[0].state.data.state,  Order.State.SETTLED)
        assertEquals(settledOrders[1].state.data.state,  Order.State.SETTLED)
    }
}