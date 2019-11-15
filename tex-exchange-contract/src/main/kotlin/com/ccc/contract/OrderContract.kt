package com.ccc.contract

import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class OrderContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        @JvmStatic
        val CONTRACT_REF = "com.example.contract.SellOrderContract"
    }

}