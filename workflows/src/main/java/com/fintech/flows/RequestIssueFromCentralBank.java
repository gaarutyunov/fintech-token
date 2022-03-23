package com.fintech.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.fintech.constants.FintechTokenConstants;
import com.fintech.states.FintechTokenType;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilities;
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensUtilities;
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow;
import com.r3.corda.lib.tokens.workflows.utilities.FungibleTokenBuilder;
import net.corda.core.contracts.Amount;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This flow allows Commercial bank to request Central bank to issue it some amount of Fintech token.
 */
public interface RequestIssueFromCentralBank {
    /**
     * Flow initiated by Commercial bank
     */
    @InitiatingFlow
    @StartableByRPC
    class IssueRequestFlow extends FlowLogic<SignedTransaction> {
        private final double amount;

        public IssueRequestFlow(final double amount) {
            this.amount = amount;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            final Party centralBank = getServiceHub().getNetworkMapCache().getPeerByLegalName(FintechTokenConstants.CENTRAL_BANK);
            if (centralBank == null) {
                throw new FlowException("No Central bank on the network");
            }

            final Party commercialBank = getServiceHub().getNetworkMapCache().getPeerByLegalName(FintechTokenConstants.COMMERCIAL_BANK);
            if (commercialBank == null) {
                throw new FlowException("There is no Commercial bank on the network");
            }

            final Party notary = getServiceHub().getNetworkMapCache().getNotary(FintechTokenConstants.NOTARY);
            if (notary == null) {
                throw new FlowException("No Notary on the network");
            }

            final Party holder = getOurIdentity();

            if (!commercialBank.equals(holder)) {
                throw new FlowException("We must be a central bank to issue tokens");
            }

            final FlowSession centralBankSession = initiateFlow(centralBank);

            // create a transaction with token issue request
            final TransactionBuilder txBuilder = new TransactionBuilder(notary);

            final FintechTokenType tokenType = new FintechTokenType();

            final FungibleToken fungibleToken = new FungibleTokenBuilder()
                    .ofTokenType(tokenType)
                    .withAmount(amount)
                    .issuedBy(centralBank)
                    .heldBy(holder)
                    .buildFungibleToken();

            IssueTokensUtilities.addIssueTokens(txBuilder, Collections.singletonList(fungibleToken));

            // collect signatures
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder,
                    getOurIdentity().getOwningKey());
            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx,
                    Collections.singletonList(centralBankSession)));

            // Finalise the transaction
            final SignedTransaction notarised = subFlow(new FinalityFlow(
                    fullySignedTx, Collections.singletonList(centralBankSession)));

            // Distribute updates
            subFlow(new UpdateDistributionListFlow(notarised));

            return notarised;
        }
    }

    /**
     * Flow handled by Central bank on TECH token issue request
     */
    @InitiatedBy(IssueRequestFlow.class)
    class IssueOnRequestHandler extends FlowLogic<SignedTransaction> {
        @NotNull
        private final FlowSession commercialBankSession;

        public IssueOnRequestHandler(final @NotNull FlowSession commercialBankSession) {
            this.commercialBankSession = commercialBankSession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            // sign commercial bank issue request
            final SecureHash signedTxId = subFlow(new SignTransactionFlow(commercialBankSession) {
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    final List<FungibleToken> tokenOutputs = stx.getCoreTransaction().getOutputStates()
                            .stream().filter(it -> it instanceof FungibleToken)
                            .map(it -> (FungibleToken) it)
                            .collect(Collectors.toList());

                    if (tokenOutputs.size() != 1) {
                        throw new FlowException("Outputs should contain 1 fungible token");
                    }

                    // this logic is for demonstration purposes
                    if (tokenOutputs.get(0).getAmount()
                            .compareTo(AmountUtilities.amount(1_000_000, tokenOutputs.get(0).getIssuedTokenType())) > 0) {
                        throw new FlowException("Cannot issue more than 1 million TECH at a time");
                    }
                }
            }).getId();

            // Finalise the transaction.
            return subFlow(new ReceiveFinalityFlow(commercialBankSession, signedTxId));
        }
    }
}
