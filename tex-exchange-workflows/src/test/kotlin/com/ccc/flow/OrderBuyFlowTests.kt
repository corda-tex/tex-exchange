package com.ccc.flow

import net.corda.core.contracts.Amount
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

class OrderBuyFlowTests {
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
    fun `OrderBuyFlow buys cheapest stock from order book`() {

    }

}