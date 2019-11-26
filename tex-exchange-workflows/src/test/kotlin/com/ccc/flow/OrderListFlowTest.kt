package com.ccc.flow

import com.ccc.state.Order
import com.ccc.state.Stock
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.utilities.getOrThrow
import net.corda.finance.AMOUNT
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.collections.HashMap
import kotlin.test.assertEquals


class OrderListFlowTest {
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

/*    init {
        listOf(dealerNodeOne,dealerNodeTwo).forEach {
            it.registerInitiatedFlow(BroadcastTransactionResponder::class.java)
            it.registerInitiatedFlow(OrderListFlowResponder::class.java)
        }
        playerNodeMap[dealerNodeOne.info.legalIdentities.first()] = dealerNodeOne
        playerNodeMap[dealerNodeTwo.info.legalIdentities.first()] = dealerNodeTwo
    }*/



    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `publish sell order for issue 1 IBM stock`() {
      //  dealerNodeTwo.registerInitiatedFlow(BroadcastTransactionFlow::class.java)

        val stock = selfIssue()
        val stockPrice =  Amount.fromDecimal(BigDecimal.ONE, Currency.getInstance(Locale.getDefault()), RoundingMode.DOWN)
        val orderListFlow = OrderListFlow(stock.linearId, stockPrice, stock.count, Instant.now().plus(Duration.ofDays(1)))
        dealerNodeOne.startFlow(orderListFlow)
        network.runNetwork()
        val order = dealerNodeOne.services.vaultService.queryBy(Order::class.java)
        assert(order.states.isNotEmpty())
    }

    private fun selfIssue() : Stock {
        val flow = SelfIssueStockFlow("IBM ", "IBM", 10)
        dealerNodeOne.startFlow(flow)

        network.runNetwork()

        val stockState = dealerNodeOne.services.vaultService.queryBy(Stock::class.java)
        return stockState.states[0].state.data
    }


}