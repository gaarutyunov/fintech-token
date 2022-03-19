package com.fintech.states;

import com.r3.corda.lib.tokens.contracts.types.TokenType;

public class FintechTokenType extends TokenType {
    public static final String IDENTIFIER = "TECH";
    public static final int FRACTION_DIGITS = 5;

    public FintechTokenType() {
        super(IDENTIFIER, FRACTION_DIGITS);
    }
}
