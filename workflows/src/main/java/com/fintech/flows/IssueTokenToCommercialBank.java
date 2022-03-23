package com.fintech.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.fintech.constants.FintechTokenConstants;
import com.fintech.states.FintechTokenType;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilitiesKt;
import com.r3.corda.lib.tokens.contracts.utilities.TransactionUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

/**
 * By this flow Central bank can directly issue some amount of TECH token to Commercial bank.
 */
@StartableByRPC
public class IssueTokenToCommercialBank extends FlowLogic<SignedTransaction> {
    @NotNull
    private final AbstractParty counterParty;
    private final double amount;

    public IssueTokenToCommercialBank(final @NotNull AbstractParty counterParty, final double amount) {
        this.counterParty = counterParty;
        this.amount = amount;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        final Party centralBank = getServiceHub().getNetworkMapCache().getPeerByLegalName(FintechTokenConstants.CENTRAL_BANK);
        if (centralBank == null) {
            throw new FlowException("There is no Central bank on the network");
        }

        if (!centralBank.equals(getOurIdentity())) {
            throw new FlowException("We must be a central bank to issue tokens");
        }

        final Party commercialBank = getServiceHub().getNetworkMapCache().getPeerByLegalName(FintechTokenConstants.COMMERCIAL_BANK);
        if (commercialBank == null) {
            throw new FlowException("There is no Commercial bank on the network");
        }

        if (!commercialBank.equals(counterParty)) {
            throw new FlowException("Central bank can only issue tokens to Commercial bank");
        }

        final Party issuer = getOurIdentity();

        final FintechTokenType tokenType = new FintechTokenType();
        final IssuedTokenType issuedTokenType = new IssuedTokenType(issuer, tokenType);
        final Amount<IssuedTokenType> tokenAmount = AmountUtilitiesKt.amount(amount, issuedTokenType);

        final FungibleToken fungibleToken = new FungibleToken(tokenAmount, counterParty, TransactionUtilitiesKt.getAttachmentIdForGenericParam(tokenType));

        return subFlow(new IssueTokens(Collections.singletonList(fungibleToken), Collections.emptyList()));
    }
}
