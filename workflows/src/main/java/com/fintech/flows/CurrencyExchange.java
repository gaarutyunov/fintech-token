package com.fintech.flows;

import com.fintech.constants.FintechTokenConstants;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilitiesKt;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensUtilitiesKt;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.InsufficientBalanceException;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * This flow is an implementation of cross-chain swap of USD for TECH.
 * Here the customer sells a certain amount of USD for certain amount of TECH token.
 */
public interface CurrencyExchange {
    @InitiatingFlow
    class RequestExchangeFlow extends FlowLogic<SignedTransaction> {
        @NotNull
        private final Party bank;
        private final double amount;

        public RequestExchangeFlow(final @NotNull Party bank, final double amount) {
            this.bank = bank;
            this.amount = amount;
        }

        @Override
        public SignedTransaction call() throws FlowException {
            final Party commercialBank = getServiceHub().getNetworkMapCache().getPeerByLegalName(FintechTokenConstants.COMMERCIAL_BANK);
            if (commercialBank == null) {
                throw new FlowException("There is no Commercial bank on the network");
            }

            final Party notary = getServiceHub().getNetworkMapCache().getNotary(FintechTokenConstants.NOTARY);
            if (notary == null) {
                throw new FlowException("No Notary on the network");
            }

            final Party issuer = getServiceHub().getNetworkMapCache().getPeerByLegalName(FintechTokenConstants.DOLLAR_WALLET);

            if (issuer == null) {
                throw new FlowException("No source for USD currency");
            }

            final Party customer = getOurIdentity();

            final FlowSession commercialBankSession = initiateFlow(commercialBank);

            final TokenType tokenType = FiatCurrency.Companion.getInstance("USD");
            final Amount<TokenType> tokenAmount = AmountUtilitiesKt.amount(amount, tokenType);

            // checking balance
            final QueryCriteria queryCriteria = QueryUtilitiesKt.heldTokenAmountCriteria(tokenType, customer);

            final List<StateAndRef<FungibleToken>> ownedTokens = getServiceHub().getVaultService().queryBy(FungibleToken.class, queryCriteria).getStates();
            if (ownedTokens.size() == 0) throw new InsufficientBalanceException(tokenAmount);

            final BigDecimal balance = ownedTokens.stream().map(it -> it.getState().getData().getAmount().toDecimal()).reduce(BigDecimal.valueOf(0), BigDecimal::add);

            final BigDecimal diff = balance.subtract(BigDecimal.valueOf(amount));

            if (diff.compareTo(BigDecimal.ZERO) < 0) {
                throw new InsufficientBalanceException(AmountUtilitiesKt.amount(diff.negate(), tokenType));
            }

            // send exchange request to commercial bank

            final TransactionBuilder txBuilder = new TransactionBuilder(notary);
            final PartyAndAmount<TokenType> partyAndAmount = new PartyAndAmount<>(commercialBank, tokenAmount);

            MoveTokensUtilitiesKt.addMoveFungibleTokens(txBuilder, getServiceHub(), Collections.singletonList(partyAndAmount), customer);

            //TODO: see example https://github.com/corda/corda-training-code/blob/master/030-tokens-sdk/workflows/src/main/java/com/template/car/flow/AtomicSale.java

            //TODO: exchange rates with commercial bank

            //TODO: sign transaction

            //TODO: collection signatures

            //TODO: finalize transaction

            return null;
        }
    }

    @InitiatedBy(RequestExchangeFlow.class)
    class ExchangeOnRequestHandler extends FlowLogic<SignedTransaction> {
        @NotNull
        private final FlowSession customerSession;

        public ExchangeOnRequestHandler(final @NotNull FlowSession customerSession) {
            this.customerSession = customerSession;
        }

        @Override
        public SignedTransaction call() throws FlowException {
            // TODO: verify transaction

            // TODO: finalize transaction
            return null;
        }
    }
}
