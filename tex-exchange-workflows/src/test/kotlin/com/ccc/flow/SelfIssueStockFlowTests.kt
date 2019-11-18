package com.ccc.flow

import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test

class SelfIssueStockFlowTests {
    //TODO: Check with Ramiz on how to use the non-deprecated Mock Network
    private val network = MockNetwork(listOf("com.ccc"))
    private val buyer = network.createNode()
    private val seller = network.createNode()

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `dummy test`() {
        true
    }
}