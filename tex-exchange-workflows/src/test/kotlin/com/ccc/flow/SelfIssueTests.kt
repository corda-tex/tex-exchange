package com.ccc.flow


import com.ccc.flow.TestUtils.selfIssueCash
import com.ccc.state.Stock
import net.corda.core.identity.CordaX500Name
import net.corda.finance.contracts.asset.Cash
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class SelfIssueTests {

    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.ccc.contract"),
                TestCordapp.findCordapp("com.ccc.flow"),
                TestCordapp.findCordapp("net.corda.finance.contracts.asset"))))

    private val issueNode = network.createNode(CordaX500Name("issuer", "", "GB"))

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `SelfIssueStockFlow issues stock`() {
        val flow = SelfIssue.SelfIssueStockFlow("IBM ", "IBM", 1)
        val stockId = issueNode.startFlow(flow).get() // Future#get waits for the flow to be completed.
        val stock = issueNode.services.vaultService.queryBy(Stock::class.java).states[0].state.data
        assertEquals(stock.code, "IBM")
        assertEquals(stock.linearId, stockId)
    }

    @Test
    fun `SelfIssueCashFlow issues cash`() {
        val cashUnits: Int = 10
        val flow = SelfIssue.SelfIssueCashFlow(cashUnits)
        issueNode.startFlow(flow).get()
        val issuedCash = issueNode.services.vaultService.queryBy(Cash.State::class.java).states[0].state.data
        assertEquals(cashUnits.toBigDecimal(), issuedCash.amount.toDecimal().setScale(0))
    }

}