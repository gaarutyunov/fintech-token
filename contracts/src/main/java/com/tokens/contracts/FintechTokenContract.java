package com.tokens.contracts;

import com.r3.corda.lib.tokens.contracts.FungibleTokenContract;
import net.corda.core.contracts.Contract;

public class FintechTokenContract extends FungibleTokenContract implements Contract {
    public static final String CONTRACT_ID = "com.fintech.FintechTokenContract";
}
