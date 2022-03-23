package com.fintech.constants;

import net.corda.core.identity.CordaX500Name;

public interface FintechTokenConstants {
    CordaX500Name NOTARY = CordaX500Name.parse("O=Notary,L=Moscow,C=RU");
    CordaX500Name DOLLAR_WALLET = CordaX500Name.parse("O=DollarWallet,L=Moscow,C=RU");
    CordaX500Name CENTRAL_BANK = CordaX500Name.parse("O=FintechCentralBank,L=Moscow,C=RU");
    CordaX500Name COMMERCIAL_BANK = CordaX500Name.parse("O=FintechCommercialBank,L=Moscow,C=RU");
    CordaX500Name CUSTOMER_A = CordaX500Name.parse("O=CustomerA,L=Moscow,C=RU");
    CordaX500Name CUSTOMER_B = CordaX500Name.parse("O=CustomerB,L=Moscow,C=RU");
}
