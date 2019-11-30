package com.ccc.contract

import com.ccc.state.Order
import com.ccc.state.Stock
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
                    "Only one input should be consumed when opening/listing a Sell order" using (1 == tx.inputStates.size)
                    "The input state type should be Stock" using (tx.inputStates.single() is Stock)
                    "Only two output states should be created when opening a Sell Order" using (2 == tx.outputStates.size)
                    val orderState = tx.outputsOfType(Order::class.java).single()
                    val stockState = tx.inputsOfType(Stock::class.java).single()
                    "A newly issued Sell Order must have a starting price greater than zero" using (orderState.price.quantity > 0)
                    val time = timeWindow?.fromTime
                        ?: throw IllegalArgumentException("Sell Order openings or listings must be timestamped")
                    "The expiry date cannot be in the past" using (time < orderState.expiryDateTime)
                    "Only the seller needs to sign the transaction" using (signers == setOf(orderState.seller.owningKey))
                    "The Sell Order must have no buyer when listed" using (orderState.buyer == null)
                    "Only the owner of the Stock can list it in a Sell Order" using (stockState.owner == orderState.seller)
                    val stockCommand = tx.commandsOfType(StockContract.Commands::class.java).single()
                    "Stock must be listed" using (stockCommand.value is StockContract.Commands.List)
                }
            }
        }

        class Buy : TypeOnlyCommandData(), Commands {
            override fun verifyCommand(tx: LedgerTransaction, signers: Set<PublicKey>) {
                val timeWindow: TimeWindow? = tx.timeWindow
                requireThat {
                    val time = timeWindow?.fromTime ?: throw IllegalArgumentException("Sell Order must be timestamped")
                    "A Buy transaction should only have one input state" using (1 == tx.inputStates.size)
                    "A Buy transaction should only have one output state" using (1 == tx.outputStates.size)
                    "The input state must be an Order" using (tx.inputStates.single() is Order)
                    "The output state must be an Order" using (tx.outputStates.single() is Order)
                    val inputOrderState = tx.inputStates.single() as Order
                    val outputOrderState = tx.outputStates.single() as Order
                    "The order must not be expired" using (time < inputOrderState.expiryDateTime)
                    "The buyer cannot be 'null'" using (outputOrderState.buyer != null)
                    "Only the 'buyer' may change" using (inputOrderState == outputOrderState.copy(buyer = inputOrderState.buyer))
                    "The 'buyer' property must change in a buy" using (outputOrderState.buyer != inputOrderState.buyer)
                    //TODO: Check this with Ramiz
                    "The seller and new buyer only must sign a buy transaction" using (signers == listOfNotNull(
                        outputOrderState.seller.owningKey,
                        requireNotNull(outputOrderState.buyer).owningKey
                    ).toSet())
                    "Stock must not change" using (tx.commandsOfType(StockContract.Commands::class.java).none())
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

        class Cancel : TypeOnlyCommandData(), Commands {
            override fun verifyCommand(tx: LedgerTransaction, signers: Set<PublicKey>) {
                val timeWindow: TimeWindow? = tx.timeWindow
                requireThat {
                    //Shape Constraints
                      //Number of Commands
                      //Number inputs
                    "There must be only one Order input" using (tx.inputsOfType(Order::class.java).size == 1)
                   // "There must zero Order outputs" using tx.outputsOfType(Order::class.java).none()
                    timeWindow?.fromTime ?: throw IllegalArgumentException("Transaction must be timestamped")

                    //Content Constraints
                    val stockCommand = tx.commandsOfType(StockContract.Commands::class.java).single()
                    "StockCommand must be of type Delist" using (stockCommand.value is StockContract.Commands.Delist)

                    //Required Signers
                    val inputOrder = tx.inputsOfType(Order::class.java).single()
                    "Seller must sign cancel transaction" using (signers == setOf(inputOrder.seller.owningKey))
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