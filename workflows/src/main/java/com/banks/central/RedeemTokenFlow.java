package com.banks.central;

import antlr.Token;
import co.paralleluniverse.fibers.Suspendable;
import com.constants.FintechTokenConstants;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.flows.rpc.RedeemFungibleTokens;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import com.tokens.fintech.FintechToken;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.identity.Party;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;

import java.util.Collections;

class RedeemTokenFlow extends FlowLogic<SignedTransaction> {
    private final double amount;

    RedeemTokenFlow(double amount) {
        this.amount = amount;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        final FintechToken fintechToken = new FintechToken();
        final Party centralBank = getServiceHub().getNetworkMapCache().getPeerByLegalName(FintechTokenConstants.CENTRAL_BANK);
        if (centralBank == null) throw new FlowException("No Central Bank found");

        final QueryCriteria heldByCommercialBank = QueryUtilitiesKt.heldTokenAmountCriteria(fintechToken, getOurIdentity());
        final Amount<TokenType> tokenAmount = AmountUtilitiesKt.amount(amount, fintechToken);

        return subFlow(new RedeemFungibleTokens(tokenAmount,
                centralBank,
                Collections.emptyList(),
                heldByCommercialBank,
                getOurIdentity()));
    }
}
