<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Fintech Token (TECH)

# Usage

1. First you need to build the project using:

```bash
./gradlew deployNodes
```

2. Then run the nodes:

```bash
./build/nodes/runnodes
```

3. Then you can use interactive shell to use following flows:

```
com.fintech.flows.CurrencyExchange$RequestExchangeFlow
com.fintech.flows.IssueFiatCurrencyToCustomer
com.fintech.flows.IssueTokenToCommercialBank
com.fintech.flows.RedeemIssuedToken
com.fintech.flows.RequestIssueFromCentralBank$IssueRequestFlow
com.fintech.flows.RequestRedeemFromCentralBank$RequestRedeemFlow
```

## IssueFiatCurrencyToCustomer

First you need to issue some USD tokens to customer, so he could perform a cross-swap (CurrencyExchange flow).
Run the following command in the interactive shell of CustomerA or CustomerB:

```
flow start com.fintech.flows.IssueFiatCurrencyToCustomer amount: 100
```

## IssueTokenToCommercialBank

Now you need to issue some TECH tokens to CommercialBank, so it could exchange them for USD afterwards. 
Run following command in the shell of CentralBank.

```
flow start com.fintech.flows.IssueTokenToCommercialBank counterParty: "O=FintechCommercialBank,L=Russia,C=RU", amount: 10000
```

## RequestIssueFromCentralBank

Also being a CommercialBank you can request CentralBank to issue you some TECH tokens:

```
flow start com.fintech.flows.RequestRedeemFromCentralBank$IssueRequestFlow amount: 1000
```

## CurrencyExchange

This is the most interesting flow of the application. 
Being a customer (CustomerA or CustomerB) you can come to CommercialBank and sell it some USD tokens in exchange for TECH tokens:

```
flow start com.fintech.flows.CurrencyExchange$CurrencyExchange amount: 50
```

**Note that `amount` parameter is the amount of USD tokens that you are selling!**

## RedeemIssuedToken

CentralBank can redeem previously issues tokens from CommercialBank using this flow:

```
flow start com.fintech.flows.RedeemIssuedToken amount: 50, counterParty: "O=FintechCommercialBank,L=Russia,C=RU"
```

## RequestRedeemFromCentralBank

Also a CommercialBank can request some previously issued tokens:

```
flow start com.fintech.flows.RequestRedeemFromCentralBank$RequestRedeemFlow amount: 50
```