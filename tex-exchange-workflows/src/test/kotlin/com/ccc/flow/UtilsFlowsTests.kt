package com.ccc.flow

import com.ccc.flow.TestUtils.selfIssueCash
import net.corda.core.identity.CordaX500Name
import net.corda.finance.contracts.asset.Cash
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class UtilsFlowsTests {

    private val network = MockNetwork(
        MockNetworkParameters(
            cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.ccc.contract"),
                TestCordapp.findCordapp("com.ccc.flow"),
                TestCordapp.findCordapp("net.corda.finance.contracts.asset")
            )
        )
    )

    private val issueNode = network.createNode(CordaX500Name("issuer", "", "GB"))

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `MergeCashFlow with no input states`() {
        val flow = UtilsFlows.MergeCashFlow()
        issueNode.startFlow(flow).get()
    }

    @Test
    fun `MergeStockFlow merges` () {

    }

    @Test
    fun `MergeCashFlow merges` () {
        val cashUnits: Int = 10
        selfIssueCash(issueNode, cashUnits)
        selfIssueCash(issueNode, cashUnits)
        // Lets merge Cash now.
        val flow = UtilsFlows.MergeCashFlow()
        val future = issueNode.startFlow(flow)
        network.runNetwork()
        future.get()
        val issuedCash = issueNode.services.vaultService.queryBy(Cash.State::class.java).states[0].state.data
        assertEquals(20.toBigDecimal(), issuedCash.amount.toDecimal().setScale(0))
    }

    @Test
    fun `SplitStockFlow merges` () {

    }

    @Test
    fun `SplitCashFlow merges` () {

    }

}