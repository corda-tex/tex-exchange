package com.ccc.flow

import com.ccc.state.Stock
import net.corda.core.contracts.UniqueIdentifier
import net.corda.testing.node.StartedMockNode

object TestUtils {

    fun issueCashToNode(issuingNode: StartedMockNode, cashUnits: Int) {
        val flow = SelfIssue.SelfIssueCashFlow(cashUnits)
        issuingNode.startFlow(flow).get()
    }

    fun issueStockToNode(issuingNode: StartedMockNode, stockId: UniqueIdentifier?, description: String, quantity: Long): UniqueIdentifier {
        val flow = SelfIssue.SelfIssueStockFlow(stockId, description, quantity)
        return issuingNode.startFlow(flow).get()
    }
}