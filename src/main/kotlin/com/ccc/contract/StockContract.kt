package com.example.contract

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

class StockContract : Contract {

    companion object {
        @JvmStatic
        val CONTRACT_REF = "com.example.contract.StockContract"
    }

    interface Commands : CommandData
    class Create : TypeOnlyCommandData(), Commands

    override fun verify(tx: LedgerTransaction) {

    }



}