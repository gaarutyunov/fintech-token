package com.tokens.fintech;

import com.r3.corda.lib.tokens.contracts.types.TokenType;

import java.util.Objects;

public class FintechToken extends TokenType {
    public static final String IDENTIFIER = "TECH";
    public static final int FRACTION_DIGITS = 5;

    public FintechToken() {
        super(IDENTIFIER, FRACTION_DIGITS);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return Objects.hash("FintechToken");
    }
}
