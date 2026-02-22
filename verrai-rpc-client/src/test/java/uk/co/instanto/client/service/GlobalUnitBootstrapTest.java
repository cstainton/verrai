package uk.co.instanto.client.service;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.concurrent.atomic.AtomicBoolean;
import uk.co.instanto.client.service.dto.proto.NodeAnnouncedEvent;
import uk.co.instanto.client.service.GlobalUnitBootstrap.SimulatedUnit;

public class GlobalUnitBootstrapTest {

    @Test
    public void testCreateSimulatedUnit() {
        String nodeId = "test-node";
        SimulatedUnit unit = GlobalUnitBootstrap.createSimulatedUnit(nodeId);

        assertNotNull("SimulatedUnit should not be null", unit);
        assertEquals("Node ID should match", nodeId, unit.nodeId);
        assertNotNull("Registry should not be null", unit.registry);
        assertNotNull("EventBus should not be null", unit.eventBus);
        assertNotNull("LocalTransport should not be null", unit.localTransport);

        // Verify relationships
        assertSame("Registry should have the same EventBus", unit.eventBus, unit.registry.getEventBus());

        // Cannot verify localNodeId easily as no getter, but can verify later via discovery
    }

    @Test
    public void testStartDiscoveryAnnouncesNode() {
        String nodeId = "discovery-node";
        SimulatedUnit unit = GlobalUnitBootstrap.createSimulatedUnit(nodeId);

        AtomicBoolean announced = new AtomicBoolean(false);

        // Subscribe to NodeAnnouncedEvent to verify it's published
        // This also verifies that the identity codec is registered correctly
        unit.eventBus.subscribe(NodeAnnouncedEvent.class, event -> {
            if (event.nodeId.equals(nodeId)) {
                announced.set(true);
            }
        });

        unit.startDiscovery();

        assertTrue("Should have received NodeAnnouncedEvent for the local node", announced.get());
    }
}
