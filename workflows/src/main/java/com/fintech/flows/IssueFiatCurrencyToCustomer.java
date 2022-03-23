package com.fintech.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.fintech.constants.FintechTokenConstants;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilities;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.utilities.FungibleTokenBuilder;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;

import java.util.Collections;

/**
 * To perform a cross-swap we must first issue fiat currency token to a customer
 * that wants to purchase a token.
 */
@StartableByRPC
public class IssueFiatCurrencyToCustomer extends FlowLogic<SignedTransaction> {
    private final double amount;

    public IssueFiatCurrencyToCustomer(final double amount) {
        this.amount = amount;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        final Party holder = getOurIdentity();
        final Party issuer = getServiceHub().getNetworkMapCache().getPeerByLegalName(FintechTokenConstants.DOLLAR_WALLET);

        if (issuer == null) {
            throw new FlowException("No source for USD currency");
        }

        if (holder == getServiceHub().getNetworkMapCache().getPeerByLegalName(FintechTokenConstants.CENTRAL_BANK)
                || holder == getServiceHub().getNetworkMapCache().getPeerByLegalName(FintechTokenConstants.COMMERCIAL_BANK)) {
            throw new FlowException("Our banks cannot issue fiat currencies for themselves");
        }

        final TokenType tokenType = FiatCurrency.Companion.getInstance("USD");

        final FungibleToken fungibleToken = new FungibleTokenBuilder()
                .ofTokenType(tokenType)
                .withAmount(amount)
                .issuedBy(issuer)
                .heldBy(holder)
                .buildFungibleToken();

        return subFlow(new IssueTokens(Collections.singletonList(fungibleToken), Collections.emptyList()));
    }
}
