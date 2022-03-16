package com.constants;

import net.corda.core.identity.CordaX500Name;

public interface FintechTokenConstants {
    CordaX500Name NOTARY = CordaX500Name.parse("O=Notary,L=Russia,C=RU");
    CordaX500Name CENTRAL_BANK = CordaX500Name.parse("O=Fintech Central Bank,L=Russia,C=RU");
    CordaX500Name COMMERCIAL_BANK = CordaX500Name.parse("O=Fintech Commercial Bank,L=Russia,C=RU");
    CordaX500Name CUSTOMER_A = CordaX500Name.parse("O=Customer A,L=Russia,C=RU");
    CordaX500Name CUSTOMER_B = CordaX500Name.parse("O=Customer B,L=Russia,C=RU");
}
