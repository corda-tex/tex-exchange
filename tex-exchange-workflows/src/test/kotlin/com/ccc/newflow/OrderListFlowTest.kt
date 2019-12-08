package com.ccc.newflow

import com.ccc.flow.OrderFlow
import com.ccc.flow.OrderListFlow
import com.ccc.flow.StockTokenFlow
import com.ccc.state.Order
import com.ccc.state.Order2
import com.ccc.util.Constants
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.collections.HashMap

class OrderListFlowTest {

    /**
     * Create Mock Network
     */
    private val network = MockNetwork(
        MockNetworkParameters(
            cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                TestCordapp.findCordapp("com.ccc.contract"),
                TestCordapp.findCordapp("com.ccc.flow")
            )
        )
    )
    private val dealerNodeOne = network.createNode(CordaX500Name("dealerNodeOne", "", "GB"))
    private val dealerNodeTwo = network.createNode(CordaX500Name("dealerNodeTwo", "", "GB"))
    private val bankOfEngland = network.createNode(CordaX500Name("BankOfEngland", "London", "GB"))
    private val partyNodeMap = HashMap<StartedMockNode, Party>()

    init {
        partyNodeMap[dealerNodeOne] = dealerNodeOne.info.legalIdentities.first()
        partyNodeMap[dealerNodeTwo] = dealerNodeTwo.info.legalIdentities.first()
        partyNodeMap[bankOfEngland] = bankOfEngland.info.legalIdentities.first()
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    @Ignore("Hanging on Notary")
    fun `publish sell order for issue 100 GOOG stock`() {
        //Given
        val ticker = "GOOG"
        val quantity = 100
        val price = BigDecimal(25)
        selfIssueStock(ticker, quantity)
        var orderListFlow2 = OrderFlow.OrderListFlow2("GOOG", 100, price, Instant.now().plus(Duration.ofDays(1)))
        //When
        dealerNodeOne.startFlow(orderListFlow2).getOrThrow()
        network.runNetwork()
        //Then
        val order = dealerNodeOne.services.vaultService.queryBy(Order2::class.java).states[0].state.data
        assert(order != null)

    }

    private fun selfIssueStock(ticker: String, quantity: Int) {
        dealerNodeOne
            .startFlow(StockTokenFlow.SelfIssueStockTokenFlow(ticker, quantity))
            .getOrThrow()
    }

}