package com.ccc.flow


import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.collections.HashMap


class SelfIssueStockFlowTests {

    /**
     * Create Mock Network
     */
    private val network = MockNetwork(
        MockNetworkParameters(
            cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.ccc.contract"),
                TestCordapp.findCordapp("com.ccc.flow")
            )
        )
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
    fun `self issue 1 IBM stock`() {
        val selfIssueStockFlow = SelfIssueStockFlow("IBM", "IBM", 1)
        val future = dealerNodeOne.startFlow(selfIssueStockFlow)
        network.runNetwork()
        val result = future.toCompletableFuture().get()
        assertThat(result.id).isInstanceOf(UUID::class.java)
    }
}