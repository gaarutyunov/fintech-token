package com.banks.central;

import co.paralleluniverse.fibers.Suspendable;
import com.constants.FintechTokenConstants;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilitiesKt;
import com.r3.corda.lib.tokens.money.DigitalCurrency;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import com.tokens.fintech.FintechToken;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.identity.Party;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

class MoveTokenFlow extends FlowLogic<SignedTransaction> {
    @NotNull
    private final Party customer;
    private final double amount;

    MoveTokenFlow(@NotNull Party customer, double amount) {
        this.customer = customer;
        this.amount = amount;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        final FintechToken tokenType = new FintechToken();
        final Party centralBank = getServiceHub().getNetworkMapCache().getPeerByLegalName(FintechTokenConstants.CENTRAL_BANK);
        if (centralBank == null) throw new FlowException("No Central Bank found");

        final Amount<TokenType> fintechTokenAmount = AmountUtilitiesKt.amount(amount, tokenType);
        final PartyAndAmount<TokenType> customersAmount = new PartyAndAmount<>(customer, fintechTokenAmount);

        final QueryCriteria issuedByCentralBank = QueryUtilitiesKt.tokenAmountWithIssuerCriteria(tokenType, centralBank);
        final QueryCriteria heldByCommercialBank = QueryUtilitiesKt.heldTokenAmountCriteria(tokenType, getOurIdentity());

        return subFlow(new MoveFungibleTokens(Collections.singletonList(customersAmount),
                Collections.emptyList(),
                issuedByCentralBank.and(heldByCommercialBank),
                getOurIdentity()));
    }
}
