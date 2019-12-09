package com.ccc.flow

import net.corda.core.identity.CordaX500Name
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test

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
    fun `MergeStockFlow merges` () {

    }

    @Test
    fun `MergeCashFlow merges` () {

    }

    @Test
    fun `SplitStockFlow merges` () {

    }

    @Test
    fun `SplitCashFlow merges` () {

    }

}