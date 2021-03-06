package com.fintech.flows;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeHandle;
import net.corda.testing.driver.NodeParameters;
import net.corda.testing.node.TestCordapp;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static net.corda.testing.driver.Driver.driver;

public class IssueFiatCurrencyTest {
    private final TestIdentity customerAIdentity = new TestIdentity(new CordaX500Name("CustomerA", "Moscow", "RU"));
    private final TestIdentity dollarWalletIdentity = new TestIdentity(new CordaX500Name("DollarWallet", "Moscow", "RU"));

    @Test
    public void issueTest() {
        driver(new DriverParameters()
                .withStartNodesInProcess(true)
                .withIsDebug(true)
                .withCordappsForAllNodes(ImmutableList.of(TestCordapp.findCordapp("com.fintech.flows"),
                        TestCordapp.findCordapp("com.fintech.contracts"),
                        TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"))), dsl -> {
            // Start the nodes and wait for them both to be ready.
            List<CordaFuture<NodeHandle>> handleFutures = ImmutableList.of(
                    dsl.startNode(new NodeParameters().withProvidedName(customerAIdentity.getName())),
                    dsl.startNode(new NodeParameters().withProvidedName(dollarWalletIdentity.getName()))
            );

            NodeHandle customerAHandle;

            try {
                customerAHandle = handleFutures.get(0).get();
            } catch (Exception e) {
                throw new RuntimeException("Caught exception during test: ", e);
            }

            try {
                customerAHandle.getRpc().startTrackedFlowDynamic(IssueFiatCurrencyToCustomer.class, 100.).getReturnValue().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

            return null;
        });
    }
}
