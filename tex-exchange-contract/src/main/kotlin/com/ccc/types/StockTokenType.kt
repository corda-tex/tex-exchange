package com.ccc.types

import com.r3.corda.lib.tokens.contracts.types.TokenType

/**
 * Wrapper around fixed [TokenType]
 */
data class StockTokenType(
    val ticker: String,
    override val fractionDigits: Int = 0
) : TokenType(ticker, fractionDigits) {
    override fun toString() = tokenIdentifier
}