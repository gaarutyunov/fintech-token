package com.fintech.contracts;

import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

/**
 * This is included so that the CorDapp scanner auto-magically includes this JAR in the attachment store. It will remain
 * until CorDapp dependencies are properly handled in Corda 5.0.
 */
public class DummyContract implements Contract {
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
    }
}
