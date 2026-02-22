package uk.co.instanto.client.service;

import dev.verrai.rpc.common.transport.MessageHandler;
import dev.verrai.rpc.common.transport.Transport;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class UnitRegistryBenchmarkTest {

    private UnitRegistry registry;
    private static final int NUM_NODES = 100;
    private static final int SERVICES_PER_NODE = 1000;

    @Before
    public void setUp() {
        registry = new UnitRegistry();
    }

    @Test
    public void benchmarkRemoveNode() {
        // Setup: Register many services across many nodes
        MockTransport transport = new MockTransport();
        List<String> nodeIds = new ArrayList<>();

        long setupStart = System.currentTimeMillis();
        for (int i = 0; i < NUM_NODES; i++) {
            String nodeId = "Node-" + i;
            nodeIds.add(nodeId);
            for (int j = 0; j < SERVICES_PER_NODE; j++) {
                String serviceId = "Service-" + i + "-" + j;
                registry.registerRemote(serviceId, nodeId, transport);
            }
        }
        long setupEnd = System.currentTimeMillis();
        System.out.println("Setup time: " + (setupEnd - setupStart) + "ms for " + (NUM_NODES * SERVICES_PER_NODE) + " services.");

        // Benchmark removeNode
        long start = System.nanoTime();

        // Remove 10 nodes to get an average
        int nodesToRemove = 10;
        for (int i = 0; i < nodesToRemove; i++) {
            registry.removeNode(nodeIds.get(i));
        }

        long end = System.nanoTime();
        double avgTimeMs = (end - start) / (nodesToRemove * 1_000_000.0);

        System.out.println("Average removeNode time: " + String.format("%.4f", avgTimeMs) + " ms");
    }

    static class MockTransport implements Transport {
        @Override
        public void addMessageHandler(MessageHandler handler) {}

        @Override
        public void send(byte[] message) {}
    }
}
