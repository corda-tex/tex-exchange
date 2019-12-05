package com.ccc.contract

import com.ccc.types.StockTokenType
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

class OrderContract2 : Contract {

    companion object {
        @JvmStatic
        val ORDER_CONTRACT_2_REF = "com.ccc.contract.OrderContract2"
    }

    interface Commands : CommandData {
        fun verifyCommand(tx : LedgerTransaction, signers: Set<PublicKey>)

        // Sell Order Open for Sale
        class List : TypeOnlyCommandData(), Commands {
            override fun verifyCommand(tx: LedgerTransaction, signers: Set<PublicKey>) {
                val timeWindow: TimeWindow? = tx.timeWindow
                requireThat {
                    "The input state type should be a FungibleToken of StockTokenType" using (tx.inputStates.all { it is FungibleToken && it.amount.token.tokenType is StockTokenType})
                    "Only two output states should be created when opening a Sell Order" using (1 == tx.outputStates.size)
                }
            }
        }

        class Buy : TypeOnlyCommandData(), Commands {
            override fun verifyCommand(tx: LedgerTransaction, signers: Set<PublicKey>) {
//                requireThat {
//
//                }
            }
        }
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        command.value.verifyCommand(tx, command.signers.toSet())
    }
}