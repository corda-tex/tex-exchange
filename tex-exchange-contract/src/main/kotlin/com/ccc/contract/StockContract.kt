package com.ccc.contract

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class StockContract : Contract {

    companion object {
        @JvmStatic
        val CONTRACT_REF = "com.ccc.contract.StockContract"
    }

    interface Commands : CommandData
    class Create : TypeOnlyCommandData(), Commands

    override fun verify(tx: LedgerTransaction) {

    }



}