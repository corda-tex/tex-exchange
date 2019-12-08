package com.ccc.newflow

import com.ccc.flow.StockTokenFlow
import com.ccc.types.StockTokenType
import com.ccc.util.Constants
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
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
import org.junit.Test
import kotlin.test.assertEquals

class StockTokenFlowTest {
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
    fun `Issue Stock`() {
        //Given
        val ticker = "GOOG"
        val quantity = 100
        val selfIssueStockTokenFlow = StockTokenFlow.SelfIssueStockTokenFlow(ticker, quantity)
        //When
        dealerNodeOne.startFlow(selfIssueStockTokenFlow).getOrThrow()
        //Then
        val stockToken = dealerNodeOne.services.vaultService.queryBy(FungibleToken::class.java).states[0].state.data
        val issuedStock = getIssuedStock(ticker, quantity)
        assertEquals(stockToken.amount, issuedStock)
    }



    private fun getIssuedStock(ticker: String, quantity: Int): Amount<IssuedTokenType> {
        val selfIssuedStock = StockTokenType(ticker, 0) issuedBy dealerNodeOne.info.legalIdentities.first()
        return  quantity of selfIssuedStock
    }

}