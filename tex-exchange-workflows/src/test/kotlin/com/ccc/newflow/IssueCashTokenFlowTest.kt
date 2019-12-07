package com.ccc.newflow

import com.ccc.flow.CashTokenFlow
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class IssueCashTokenFlowTest {

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
    private val bankOfEngland = network.createNode(CordaX500Name("BankOfEngland", "London", "GB"))
    private val playerNodeMap = HashMap<Party, StartedMockNode>()

    init {
        playerNodeMap[dealerNodeOne.info.legalIdentities.first()] = dealerNodeOne
        playerNodeMap[dealerNodeTwo.info.legalIdentities.first()] = dealerNodeTwo
        playerNodeMap[bankOfEngland.info.legalIdentities.first()] = bankOfEngland
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `Issue Cash`() {
        //Given
        var amountOfCash = BigDecimal(1000.00)
        CashTokenFlow.IssueCashTokenFlow(amountOfCash)

    }

}