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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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

    private val aNode = network.createNode(CordaX500Name("issuer", "", "GB"))

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `MergeCashFlow with no input states`() {
        val flow = UtilsFlows.MergeCashFlow()
        aNode.startFlow(flow).get()
    }

    @Test
    fun `MergeStockFlow merges` () {

    }

    @Test
    fun `MergeCashFlow merges` () {
        val cashUnits: Int = 10
        selfIssueCash(aNode, cashUnits)
        selfIssueCash(aNode, cashUnits)
        // Lets merge Cash now.
        val flow = UtilsFlows.MergeCashFlow()
        val future = aNode.startFlow(flow)
        network.runNetwork()
        future.get()
        val issuedCash = aNode.services.vaultService.queryBy(Cash.State::class.java).states[0].state.data
        assertEquals(20.toBigDecimal(), issuedCash.amount.toDecimal().setScale(0))
    }

    @Test
    fun `SplitStockFlow splits` () {

    }

    @Test
    fun `SplitCashFlow finds the amount in a state` () {
        // WORK HERE
        val cashUnits: Int = 10
        selfIssueCash(aNode, cashUnits)
        val flow = UtilsFlows.SplitAndGetCashFlow(10)
        val future = aNode.startFlow(flow) // no need to run the network for this one. Notary takes no place here.
        val existingCashState = future.get()
        assertNotNull(existingCashState)
    }

    @Test
    fun `SplitCashFlow does splitting and returns the requested cash`() {
        // Split that and get a cash(3). Check that in vault exists a cash(3) and a cash(7)
        val cashUnits: Int = 10
        selfIssueCash(aNode, cashUnits)
        val flow = UtilsFlows.SplitAndGetCashFlow(3)
        val future = aNode.startFlow(flow)
        network.runNetwork()
        val returnedCashState = future.get()
        // Check returned cash state.
        assertEquals(3.toBigDecimal(), returnedCashState?.amount?.toDecimal()?.setScale(0))
        // Check correct outcome of splitting.
        assertEquals(3.toBigDecimal(), aNode.services.vaultService.queryBy(Cash.State::class.java).states[0].state.data.amount.toDecimal()?.setScale(0))
        assertEquals(7.toBigDecimal(), aNode.services.vaultService.queryBy(Cash.State::class.java).states[1].state.data.amount.toDecimal()?.setScale(0))
    }

    @Test
    fun `SplitCashFlow does merging and returns the requested cash - Requested cash exists in vault`() {
        // Add cash: 1, 2 ,3 ask for 6. It should merge 1+2+3 = 6 and return that.
        selfIssueCash(aNode, 1)
        selfIssueCash(aNode, 2)
        selfIssueCash(aNode, 3)
        val flow = UtilsFlows.SplitAndGetCashFlow(6)
        val future = aNode.startFlow(flow)
        network.runNetwork()
        val returnedCashState = future.get()
        assertEquals(6.toBigDecimal(), returnedCashState?.amount?.toDecimal()?.setScale(0))
    }

    @Test
    fun `SplitCashFlow does merging and returns the requested cash - Requested cash does not exist in vault`() {
        // Add cash: 1, 2 ,3 ask for 6. It should merge 1+2+3 = 6 and return that.
        selfIssueCash(aNode, 1)
        selfIssueCash(aNode, 2)
        selfIssueCash(aNode, 3)
        val flow = UtilsFlows.SplitAndGetCashFlow(7)
        val future = aNode.startFlow(flow)
        network.runNetwork()
        val returnedCashState = future.get()
        assertNull(returnedCashState)
    }

}