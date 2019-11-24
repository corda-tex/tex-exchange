package com.ccc.flow

import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Questions:
 *
 *  Is there a responder flow for SelfIssue , I guess not. ?
 */

/**
 * Notes:
 *
 * The Network is made of dealer nodes. Each node can selfIssue independently.
 */


class SelfIssueStockFlowTests {

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
       /* listOf(player1, player2, dealer).forEach {
            it.registerInitiatedFlow(GameResponder::class.java)
        }*/
        playerNodeMap[dealerNodeOne.info.legalIdentities.first()] = dealerNodeOne
        playerNodeMap[dealerNodeTwo.info.legalIdentities.first()] = dealerNodeTwo
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `createMockNetwork`() {
        true
    }

    @Test
    fun `self issue 1 IBM stock`() {
        var flow = SelfIssueStockFlow("IBM ", "IBM", 1)
        val future = dealerNodeOne.startFlow(flow)
        //When
        network.runNetwork()
        //Then
        /*execute constructed flow, the call method on the acceptor flow is executed*/
        /* calls verify on Game Contract - no rules for now */
        val stx = future.getOrThrow()

    }
}