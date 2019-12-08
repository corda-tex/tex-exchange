package com.ccc.flow

import co.paralleluniverse.fibers.Suspendable
import com.ccc.types.StockTokenType
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.getAttachmentIdForGenericParam
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

object StockTokenFlow {

    /**
     * Self-issue Fungible Stock Token on ledger
     */
    @StartableByRPC
    class SelfIssueStockTokenFlow(private val ticker: String, private val quantity: Int) : FlowLogic<SignedTransaction>() {

        override val progressTracker = ProgressTracker()

        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction {
            // grab the notary
          //  val notary = serviceHub.networkMapCache.notaryIdentities.first()

            // create token type
            val stockTokenType = StockTokenType(ticker, 0)

            // assign the issuer (self)
            val selfIssuedStock = stockTokenType issuedBy ourIdentity

            // specify the amount to issue to holder
            val issueQuantity = quantity of selfIssuedStock

            // create fungible quantity specifying the new owner
            val fungibleToken = FungibleToken(issueQuantity, ourIdentity, stockTokenType.getAttachmentIdForGenericParam())

            // use built in flow for issuing tokens on ledger
            return subFlow(IssueTokens(listOf(fungibleToken)))
        }
    }

    /**
     * Move created Fungible Stock tokens to other party
     */
    @StartableByRPC
    class MoveStockTokenFlow(private val ticker: String, private val quantity: Int, private val recipient: Party) : FlowLogic<SignedTransaction>() {

        override val progressTracker = ProgressTracker()

        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction {
            return subFlow(MoveFungibleTokens(quantity of StockTokenType(ticker), recipient))
        }
    }
}