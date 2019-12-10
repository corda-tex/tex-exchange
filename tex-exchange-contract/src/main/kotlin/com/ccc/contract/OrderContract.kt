package com.ccc.contract

import com.ccc.state.Order
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

/**
 * Transactions involving one an Order will use this contract to
 * verify that the transactions are valid.
 *
 */
class OrderContract : Contract {

    companion object {
        @JvmStatic
        val ORDER_CONTRACT_REF = "com.ccc.contract.OrderContract"
    }

    /**
     * Adding more commands will require implementation of the corresponding
     * verification method.
     */
    interface Commands : CommandData {
        fun verifyCommand(tx: LedgerTransaction, signers: Set<PublicKey>)

        // Sell Order Open for Sale
        class List : TypeOnlyCommandData(), Commands {
            override fun verifyCommand(tx: LedgerTransaction, signers: Set<PublicKey>) {
                val timeWindow: TimeWindow? = tx.timeWindow
                requireThat {
                    "Only two output states should be created when opening a Sell Order" using (1 == tx.outputStates.size)
                    val sellOrder = tx.outputsOfType(Order::class.java).single()
                    "A newly issued Sell Order must have a starting price greater than zero" using (sellOrder.price.quantity > 0)
                    val time = timeWindow?.fromTime ?: throw IllegalArgumentException("Sell Order openings or listings must be timestamped")
                    "The expiry date cannot be in the past" using (time < sellOrder.expiryDateTime)
                    "Only the seller needs to sign the transaction" using (signers == setOf(sellOrder.seller.owningKey))
                    "The Sell Order must have no buyer when listed" using (sellOrder.buyer == null)
                }
            }
        }

        class Buy : TypeOnlyCommandData(), Commands {
            override fun verifyCommand(tx: LedgerTransaction, signers: Set<PublicKey>) {
                val timeWindow: TimeWindow? = tx.timeWindow
                requireThat {
                    //val time = timeWindow?.fromTime ?: throw IllegalArgumentException("Sell Order must be timestamped")
                    "A Buy transaction should only have one input state" using (1 == tx.inputStates.size)
                    "A Buy transaction should only have one output state" using (1 == tx.outputStates.size)
                    val inputOrderState = tx.inputStates.single() as Order
                    val outputOrderState = tx.outputStates.single() as Order
                    "The buyer cannot be 'null'" using (outputOrderState.buyer != null)
//                    "The seller and new buyer only must sign a buy transaction" using (signers == listOfNotNull(
//                        outputOrderState.seller.owningKey,
//                        requireNotNull(outputOrderState.buyer).owningKey
//                    ).toSet())
                }
            }
        }

        class Settle : TypeOnlyCommandData(), Commands {
            override fun verifyCommand(tx: LedgerTransaction, signers: Set<PublicKey>) {
                //TODO
            }
        }

        class End : TypeOnlyCommandData(), Commands {
            override fun verifyCommand(tx: LedgerTransaction, signers: Set<PublicKey>) {
                val timeWindow: TimeWindow? = tx.timeWindow
                requireThat {
                    "There must be only one Order input" using (tx.inputsOfType(Order::class.java).size == 1)
                    val inputOrder = tx.inputsOfType(Order::class.java).single()
                    "There must not be any Order outputs" using tx.outputsOfType(Order::class.java).none()
                    timeWindow?.fromTime ?: throw IllegalArgumentException("Transaction must be timestamped")
                    val stockCommand = tx.commandsOfType(StockContract.Commands::class.java).single()
                    "Stock must be delisted" using (stockCommand.value is StockContract.Commands.Delist)
                    if (inputOrder.buyer != null) {
                        "Both seller and buyer only must sign the Order to end transaction" using
                                (signers == listOf(inputOrder.seller.owningKey, inputOrder.buyer.owningKey).toSet())
                    } else {
                        "Only the seller must sign the order end transaction" using (signers == setOf(inputOrder.seller.owningKey))
                    }
                }
            }
        }
    }

    /**
     * The contract code for the [OrderContract].
     * The constraints are self documenting so don't require any additional explanation.
     */
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        command.value.verifyCommand(tx, command.signers.toSet())
    }
}