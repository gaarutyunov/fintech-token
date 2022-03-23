package com.fintech.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.fintech.constants.FintechTokenConstants;
import com.fintech.services.ExchangeOracle;
import com.fintech.states.FintechTokenType;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilities;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensUtilities;
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilities;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.InsufficientBalanceException;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.UntrustworthyData;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This flow is an implementation of cross-chain swap of USD for TECH.
 * Here the customer sells a certain amount of USD for certain amount of TECH token.
 */
public interface CurrencyExchange {
    /**
     * This flow is instantiated by customer to exchange token with a bank.
     */
    @InitiatingFlow
    @StartableByRPC
    class RequestExchangeFlow extends FlowLogic<SignedTransaction> {
        private final double amount;

        public RequestExchangeFlow(final double amount) {
            this.amount = amount;
        }

        @Override
        @Suspendable
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
            final Amount<TokenType> tokenAmount = AmountUtilities.amount(amount, tokenType);

            // checking balance
            final QueryCriteria queryCriteria = QueryUtilities.heldTokenAmountCriteria(tokenType, customer);

            final List<StateAndRef<FungibleToken>> ownedTokens = getServiceHub().getVaultService().queryBy(FungibleToken.class, queryCriteria).getStates();
            if (ownedTokens.size() == 0) throw new InsufficientBalanceException(tokenAmount);

            final BigDecimal balance = ownedTokens.stream().map(it -> it.getState().getData().getAmount().toDecimal()).reduce(BigDecimal.valueOf(0), BigDecimal::add);

            final BigDecimal diff = balance.subtract(BigDecimal.valueOf(amount));

            if (diff.compareTo(BigDecimal.ZERO) < 0) {
                throw new InsufficientBalanceException(AmountUtilities.amount(diff.negate(), tokenType));
            }

            // send exchange request to commercial bank

            final TransactionBuilder txBuilder = new TransactionBuilder(notary);
            final PartyAndAmount<TokenType> partyAndAmount = new PartyAndAmount<>(commercialBank, tokenAmount);

            MoveTokensUtilities.addMoveFungibleTokens(txBuilder, getServiceHub(), Collections.singletonList(partyAndAmount), customer);

            final UntrustworthyData<Double> fintechTokenAmount = commercialBankSession.sendAndReceive(Double.class, amount);

            // here we compare the amount that bank sends us to the amount proposed by the exchange rate
            final ExchangeOracle exchangeOracle = getServiceHub().cordaService(ExchangeOracle.class);

            final Double supposedRate = exchangeOracle.getExchangeRate("USD");
            final Double verifiedAmount = fintechTokenAmount.unwrap(new DoubleAmountValidator());

            if (Double.valueOf((supposedRate * amount)).compareTo(verifiedAmount) != 0) {
                throw new FlowException("The amount calculated by bank is not legit");
            }

            final FintechTokenType fintechTokenType = new FintechTokenType();
            final Amount<TokenType> techTokenAmount = AmountUtilities.amount(verifiedAmount, fintechTokenType);

            final PartyAndAmount<TokenType> fintechPartyAndAmount = new PartyAndAmount<>(commercialBank, techTokenAmount);

            MoveTokensUtilities.addMoveFungibleTokens(txBuilder, getServiceHub(), Collections.singletonList(fintechPartyAndAmount), commercialBank);

            // collect signatures
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder,
                    getOurIdentity().getOwningKey());
            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx,
                    Collections.singletonList(commercialBankSession)));

            // Finalise the transaction
            final SignedTransaction notarised = subFlow(new FinalityFlow(
                    fullySignedTx, Collections.singletonList(commercialBankSession)));

            // Distribute updates
            subFlow(new UpdateDistributionListFlow(notarised));

            return notarised;
        }
    }

    /**
     * Flow handled by Commercial bank when customer requests exchange of currency.
     */
    @InitiatedBy(RequestExchangeFlow.class)
    class ExchangeOnRequestHandler extends FlowLogic<SignedTransaction> {
        @NotNull
        private final FlowSession customerSession;

        public ExchangeOnRequestHandler(final @NotNull FlowSession customerSession) {
            this.customerSession = customerSession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            // calculate the token amount according to exchange rate
            final UntrustworthyData<Double> usdAmount = customerSession.receive(Double.class);
            final Double amount = usdAmount.unwrap(new DoubleAmountValidator());

            final ExchangeOracle exchangeOracle = getServiceHub().cordaService(ExchangeOracle.class);

            final Double fintechTokenRate = exchangeOracle.getExchangeRate("USD");
            final Double fintechTokenAmount = amount * fintechTokenRate;

            customerSession.send(fintechTokenAmount);
            // sign commercial bank issue request
            final SecureHash signedTxId = subFlow(new SignTransactionFlow(customerSession) {
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    final List<FungibleToken> tokenOutputs = stx.getCoreTransaction().getOutputStates()
                            .stream().filter(it -> it instanceof FungibleToken)
                            .map(it -> (FungibleToken) it)
                            .collect(Collectors.toList());

                    if (tokenOutputs.size() != 1) {
                        throw new FlowException("Outputs should contain 1 fungible token");
                    }

                    // verify that the amount is the previously calculated
                    if (tokenOutputs.get(0).getAmount()
                            .compareTo(AmountUtilities.amount(fintechTokenAmount, tokenOutputs.get(0).getIssuedTokenType())) != 0) {
                        throw new FlowException("This is not the amount we are ready to issue");
                    }
                }
            }).getId();

            // Finalise the transaction.
            return subFlow(new ReceiveFinalityFlow(customerSession, signedTxId));
        }
    }

    /**
     * Validator used to unwrap double amount of token during exchange.
     */
    class DoubleAmountValidator implements UntrustworthyData.Validator<Double, Double> {
        @Override
        public Double validate(Double data) throws FlowException {
            if (data == null || data.compareTo(0.) <= 0) {
                throw new FlowException("Must be a positive token amount");
            }

            return data;
        }
    }
}
