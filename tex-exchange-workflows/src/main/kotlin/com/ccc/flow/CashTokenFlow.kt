package com.ccc.flow

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.getAttachmentIdForGenericParam
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.money.GBP
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import java.math.BigDecimal
import java.math.RoundingMode

object CashTokenFlow {

    @StartableByRPC
    class IssueCashTokenFlow(private val quantity: Double): FlowLogic<SignedTransaction>() {
        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction {
            val bankOfEngland =
                serviceHub.networkMapCache.getNodesByLegalName(
                    CordaX500Name.parse("O=BankOfEngland,L=London,C=GB")).first().legalIdentities.first()
            if (ourIdentity != bankOfEngland) {
                throw IllegalArgumentException("Only Bank of England can issue GBP")
            }
            val amountOfIssuedToken = quantity of GBP issuedBy bankOfEngland
            val fungibleToken = FungibleToken(amountOfIssuedToken, ourIdentity, GBP.getAttachmentIdForGenericParam())
            return subFlow(IssueTokens(listOf(fungibleToken)))
        }
    }

    @StartableByRPC
    class MoveCashTokenFlow(private val quantity: Double, private val recipient: Party): FlowLogic<SignedTransaction>() {
        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction {
            return subFlow(
                MoveFungibleTokens(
                    amount = quantity of GBP,
                    holder = recipient
                ))
        }
    }
}