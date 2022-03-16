package com.banks.central;

import co.paralleluniverse.fibers.Suspendable;
import com.constants.FintechTokenConstants;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.tokens.fintech.FintechToken;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

class IssueTokenFLow extends FlowLogic<SignedTransaction> {
    @NotNull
    private final Party commercialBank;
    private final double amount;

    IssueTokenFLow(@NotNull Party commercialBank, double amount) {
        this.commercialBank = commercialBank;
        this.amount = amount;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        final FintechToken tokenType = new FintechToken();
        if (!getOurIdentity().getName().equals(FintechTokenConstants.CENTRAL_BANK)) {
            throw new FlowException("We are not the Central Bank");
        }
        final IssuedTokenType issuedTokenType = new IssuedTokenType(getOurIdentity(), tokenType);

        final Amount<IssuedTokenType> amountOfToken = AmountUtilitiesKt.amount(amount, issuedTokenType);
        final FungibleToken fungibleToken = new FungibleToken(amountOfToken, commercialBank, null);

        return subFlow(new IssueTokens(Collections.singletonList(fungibleToken), Collections.emptyList()));
    }
}
