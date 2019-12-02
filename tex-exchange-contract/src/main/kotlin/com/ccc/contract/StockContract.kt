package com.ccc.contract

import com.ccc.state.Order
import com.ccc.state.Stock
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

class StockContract : Contract {

    companion object {
        @JvmStatic
        val STOCK_CONTRACT_REF = "com.ccc.contract.StockContract"
    }

    /**
     * Adding more commands will require implementation of the corresponding
     * verification method. Designed this way to avoid changes to the actual [verify] method.
     */
    interface Commands : CommandData {
        fun verifyCommand(tx: LedgerTransaction, signers: Set<PublicKey>)

        class Issue : TypeOnlyCommandData(), Commands {
            override fun verifyCommand(tx: LedgerTransaction, signers: Set<PublicKey>) {
                requireThat {
                    "No inputs should be consumed when issuing a Stock item" using tx.inputStates.isEmpty()
                    "Only one output state should be created when issuing a Stock item" using (1 == tx.outputStates.size)
                    "The output state must be an Stock" using (tx.outputStates.single() is Stock)
                    val state = tx.outputStates.single() as Stock
                    "Only the owner needs to sign the transaction" using (signers == setOf(state.owner.owningKey))
                }
            }
        }

        class List : TypeOnlyCommandData(), Commands {
            override fun verifyCommand(tx: LedgerTransaction, signers: Set<PublicKey>) {
                requireThat {
                    "There must be only one Stock input" using (tx.inputsOfType(Stock::class.java).size == 1)
                    "There must be only one Stock output" using (tx.outputsOfType(Stock::class.java).size == 1)
                    val stockStateOut = tx.outputsOfType(Stock::class.java).single()
                    val stockStateIn = tx.inputsOfType(Stock::class.java).single()
                    "The 'listed' stock must be false in the input state" using (!stockStateIn.listed)
                    "The 'listed' stock must be true in the output state" using (stockStateOut.listed)
                    "Only the 'listed' stock can change" using (stockStateOut.copy(listed = false) == stockStateIn)
                    "Only the owner needs to sign the transaction" using (signers == setOf(stockStateIn.owner.owningKey))
                }
            }
        }

        /**
         *
         */

        class Transfer : TypeOnlyCommandData(), Commands {
            override fun verifyCommand(tx: LedgerTransaction, signers: Set<PublicKey>) {
                requireThat {
                    "There must be only one Stock input" using (tx.inputsOfType(Stock::class.java).size == 1)
                    "There must be only one Stock output" using (tx.outputsOfType(Stock::class.java).size == 1)
                    val inputStock = tx.inputsOfType(Stock::class.java).single()
                    val outputStock = tx.outputsOfType(Stock::class.java).single()
                    "Only the 'owner' and 'listed' properties can change" using (inputStock == outputStock.copy(
                        owner = inputStock.owner,
                        listed = inputStock.listed
                    ))
                    "The 'owner' property must change" using (outputStock.owner != inputStock.owner)
                    "The 'listed' property must change" using (outputStock.listed != inputStock.listed)
                    "The 'listed' property must be 'false'" using (!outputStock.listed)
                    "The previous and new owner only must sign a transfer transaction" using (signers == setOf(
                        outputStock.owner.owningKey,
                        inputStock.owner.owningKey
                    ))
                }
            }
        }

        class Delist : TypeOnlyCommandData(), Commands {
            override fun verifyCommand(tx: LedgerTransaction, signers: Set<PublicKey>) {
                "There must be only one Stock input" using (tx.inputsOfType(Stock::class.java).size == 1)
                "There must be only one Stock output" using (tx.outputsOfType(Stock::class.java).size == 1)
                val inputItem = tx.inputsOfType(Stock::class.java).single()
                val outputItem = tx.outputsOfType(Stock::class.java).single()
                val inputOrder = tx.inputsOfType(Order::class.java).single()
                "Only the 'listed' property can change" using (inputItem == outputItem.copy(listed = inputItem.listed))
                "The 'listed' property must change" using (outputItem.listed != inputItem.listed)
                "The 'listed' property must be 'false'" using (!outputItem.listed)
                if (inputOrder.buyer != null) {
                    "Only the current owner and buyer must sign a de-list transaction" using (signers == setOf(
                        inputItem.owner.owningKey,
                        inputOrder.buyer.owningKey
                    ))
                } else {
                    "Only the owner must sign a de-list transaction" using (signers == setOf(inputItem.owner.owningKey))
                }
            }
        }
    }

    /**
     * The contract code for the [StockContract].
     * The constraints are self documenting so don't require any additional explanation.
     */
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        command.value.verifyCommand(tx, command.signers.toSet())
    }
}