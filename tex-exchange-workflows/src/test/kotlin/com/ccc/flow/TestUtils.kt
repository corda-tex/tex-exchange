package com.ccc.flow

import net.corda.testing.node.StartedMockNode

object TestUtils {

    fun selfIssueCash(issuingNode: StartedMockNode, cashUnits: Int) {
        val flow = SelfIssue.SelfIssueCashFlow(cashUnits)
        issuingNode.startFlow(flow).get()
    }

}