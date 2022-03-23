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

import java.util.List;
import java.util.concurrent.ExecutionException;

import static net.corda.testing.driver.Driver.driver;

public class IssueTokenTest {
    private final TestIdentity commercialIdentity = new TestIdentity(new CordaX500Name("FintechCommercialBank", "Moscow", "RU"));
    private final TestIdentity centralIdentity = new TestIdentity(new CordaX500Name("FintechCentralBank", "Moscow", "RU"));

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
                    dsl.startNode(new NodeParameters().withProvidedName(commercialIdentity.getName())),
                    dsl.startNode(new NodeParameters().withProvidedName(centralIdentity.getName()))
            );

            NodeHandle commercialHandle;
            NodeHandle centralHandle;

            try {
                commercialHandle = handleFutures.get(0).get();
                centralHandle = handleFutures.get(1).get();
            } catch (Exception e) {
                throw new RuntimeException("Caught exception during test: ", e);
            }

            Party commercialBank = commercialHandle.getNodeInfo().getLegalIdentities().get(0);

            try {
                centralHandle.getRpc().startTrackedFlowDynamic(IssueTokenToCommercialBank.class, commercialBank, 100.).getReturnValue().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

            return null;
        });
    }
}
