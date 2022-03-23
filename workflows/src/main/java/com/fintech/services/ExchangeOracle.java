package com.fintech.services;

import net.corda.core.node.AppServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;

import java.util.Collections;
import java.util.Map;

/**
 * An Oracle service for retrieving exchange rates.
 * Since we only have an internal currency we define a constant exchange rate.
 */
@CordaService
public class ExchangeOracle extends SingletonSerializeAsToken {
    @SuppressWarnings("unused")
    private final AppServiceHub services;
    private static final Map<String, Double> EXCHANGE_RATES = Collections.singletonMap("USD", 0.1);

    public ExchangeOracle(AppServiceHub services) {
        this.services = services;
    }

    public Double getExchangeRate(String currencyCode) {
        return EXCHANGE_RATES.getOrDefault(currencyCode, 1.);
    }
}
