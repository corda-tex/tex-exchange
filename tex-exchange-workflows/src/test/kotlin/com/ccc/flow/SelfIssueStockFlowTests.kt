package com.ccc.flow


import com.ccc.state.Stock
import net.corda.core.identity.CordaX500Name
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class SelfIssueStockFlowTests {

    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.ccc.contract"),
                TestCordapp.findCordapp("com.ccc.flow"))))

    private val issueNode = network.createNode(CordaX500Name("issuer", "", "GB"))

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `SelfIssueStockFlow issues stock`() {
        var flow = SelfIssueStockFlow("IBM ", "IBM", 1)
        issueNode.startFlow(flow).get() // Future#get waits for the flow to be completed.
        var stockState = issueNode.services.vaultService.queryBy(Stock::class.java)
        var stock = stockState.states[0].state.data
        assertEquals(stock.code, "IBM")
        assertEquals(1, stockState.states.size)
    }

}