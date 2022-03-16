package com.tokens.states;

import com.r3.corda.lib.tokens.contracts.internal.schemas.FungibleTokenSchemaV1;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.tokens.contracts.FintechTokenContract;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.identity.AbstractParty;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

@BelongsToContract(FintechTokenContract.class)
public class FintechTokenState extends FungibleToken {
    public FintechTokenState(@NotNull Amount<IssuedTokenType> amount, @NotNull AbstractParty holder) {
        super(amount, holder, null);
    }

    @NotNull
    @Override
    public List<FungibleTokenSchemaV1> supportedSchemas() {
        return super.supportedSchemas();
    }

    public Amount<Currency> getExchangeRate() {
        // TODO: Implement Oracle service
        return Amount.fromDecimal(BigDecimal.valueOf(0.1), Currency.getInstance("USD"));
    }
}
