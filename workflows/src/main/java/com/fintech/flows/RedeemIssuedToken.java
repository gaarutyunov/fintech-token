package com.fintech.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.fintech.constants.FintechTokenConstants;
import com.fintech.states.FintechTokenType;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.flows.rpc.RedeemFungibleTokens;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

/**
 * This flow allows Central bank to redeem token amount issued to Commercial bank.
 */
@StartableByRPC
public class RedeemIssuedToken extends FlowLogic<SignedTransaction> {
    private final double amount;
    @NotNull
    private final Party counterParty;

    public RedeemIssuedToken(final double amount, final @NotNull Party counterParty) {
        this.amount = amount;
        this.counterParty = counterParty;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        final Party centralBank = getServiceHub().getNetworkMapCache().getPeerByLegalName(FintechTokenConstants.CENTRAL_BANK);
        if (centralBank == null) {
            throw new FlowException("There is no Central bank on the network");
        }

        if (!centralBank.equals(getOurIdentity())) {
            throw new FlowException("We must be a central bank to redeem tokens");
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
        final Amount<TokenType> tokenAmount = AmountUtilitiesKt.amount(amount, tokenType);

        final QueryCriteria heldByCommercialBank = QueryUtilitiesKt.heldTokenAmountCriteria(tokenType, counterParty);

        return subFlow(new RedeemFungibleTokens(
                tokenAmount, // How much to redeem
                issuer, // issuer
                Collections.emptyList(), // Observers
                heldByCommercialBank, // Criteria to find the inputs
                getOurIdentity())); // change holder
    }
}
