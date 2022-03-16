package com.banks.commercial;

import co.paralleluniverse.fibers.Suspendable;
import com.constants.FintechTokenConstants;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokensHandler;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import com.tokens.fintech.FintechToken;
import com.tokens.states.FintechTokenState;
import kotlin.Unit;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CurrencyExchange {
    @InitiatingFlow
    class CommercialBank extends FlowLogic<SignedTransaction> {
        @Override
        @Suspendable
        @NotNull
        public SignedTransaction call() throws FlowException {
            throw new NotImplementedException("");
        }
    }

    @InitiatedBy(CommercialBank.class)
    class Customer extends FlowLogic<Unit> {
        @Override
        @Suspendable
        public Unit call() throws FlowException {
            throw new NotImplementedException("");
        }
    }
}
