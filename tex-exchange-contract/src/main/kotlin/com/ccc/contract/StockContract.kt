package com.ccc.contract

import com.ccc.state.Order
import com.ccc.state.Stock
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.transactions.LedgerTransaction
import java.math.BigDecimal
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
                    "There must be only one Order output" using (tx.outputsOfType(Order::class.java).size == 1)
                }
            }
        }

        class Transfer : TypeOnlyCommandData(), Commands {
            override fun verifyCommand(tx: LedgerTransaction, signers: Set<PublicKey>) {
                requireThat {
                    "There must be only one Stock input" using (tx.inputsOfType(Stock::class.java).size == 1)
                    "There must be only one Stock output" using (tx.outputsOfType(Stock::class.java).size == 1)
                    val inputStock = tx.inputsOfType(Stock::class.java).single()
                    val outputStock = tx.outputsOfType(Stock::class.java).single()
                    "The 'owner' property must change" using (outputStock.owner != inputStock.owner)
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
                "Only the 'listed' property can change" using (inputItem == outputItem.copy(orderId = inputItem.orderId))
                "The 'listed' property must change" using (outputItem.orderId != inputItem.orderId)
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

        class Merge : Commands {
            override fun verifyCommand(tx: LedgerTransaction, signers: Set<PublicKey>) {
                "There must only be one output state" using (tx.outputStates.size == 1)
                val outState = tx.outputStates[0] as Stock
                val outAmount = outState.amount
                var sumInAmount = BigDecimal(0)
                tx.inputStates.forEach { sumInAmount += (it as Stock).amount.toDecimal() }
                "Stock amounts before and after must be equal" using (sumInAmount == outAmount.toDecimal())
            }
        }

        class Split : Commands {
            override fun verifyCommand(tx: LedgerTransaction, signers: Set<PublicKey>) {
                "There must only be one input state" using (tx.inputStates.size == 1)
                "There must only be two output states" using (tx.outputStates.size == 2)
            }
        }

        class Reserve: Commands {
            override fun verifyCommand(tx: LedgerTransaction, signers: Set<PublicKey>) {
                "There must be only one Stock input" using (tx.inputsOfType(Stock::class.java).size == 1)
                "There must be only one Stock output" using (tx.outputsOfType(Stock::class.java).size == 1)
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