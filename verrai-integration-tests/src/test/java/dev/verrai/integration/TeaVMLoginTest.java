package dev.verrai.integration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import dev.verrai.client.service.RpcClient;
import dev.verrai.client.service.UnitRegistry;
import dev.verrai.client.service.proto.RpcPacket;
import dev.verrai.rpc.common.transport.Transport;
import dev.verrai.rpc.common.transport.MessageHandler;
import dev.verrai.integration.service.LoginService;
import dev.verrai.integration.service.LoginService_Stub;
import dev.verrai.integration.service.MyDataService;
import dev.verrai.integration.service.MyDataService_Stub;
import dev.verrai.integration.service.dto.LoginRequest;
import dev.verrai.integration.service.dto.MyData;
import okio.ByteString;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(TeaVMTestRunner.class)
public class TeaVMLoginTest {

    @Test
    public void testLoginAndJwtHeader() {
        // 1. Mock Transport (Simulating Network/Server)
        MockTransport mockTransport = new MockTransport();

        // 2. Setup Client Side
        // Register remote service locations pointing to mock transport
        UnitRegistry.getInstance().registerRemote(LoginService.class.getName(), "server-node", mockTransport);
        UnitRegistry.getInstance().registerRemote(MyDataService.class.getName(), "server-node", mockTransport);

        // 3. Stubs
        LoginService loginService = new LoginService_Stub();
        MyDataService dataService = new MyDataService_Stub();

        // --- Step A: Login ---
        final StringBuilder jwtReceived = new StringBuilder();

        LoginRequest req = new LoginRequest();
        req.setUsername("user");
        req.setPassword("pass");

        loginService.login(req).thenAccept(jwt -> {
            jwtReceived.append(jwt);
            // Set JWT for subsequent calls
            UnitRegistry.getInstance().setDefaultHeader("Authorization", "Bearer " + jwt);
        }).exceptionally(ex -> {
            throw new RuntimeException("Login failed", ex);
        });

        // Mock Server Processing Login
        // The transport receives bytes, we need to manually process/reply since we don't have a real server loop here
        processNextPacket(mockTransport, packet -> {
            assertEquals("login", packet.methodName);
            // Reply with JWT
            String jwt = "mock-signed-jwt-token";
            // Encode String -> Codec -> Wire -> Packet Payload
            // String is handled by Identity Codec or simple String wrapper?
            // ServiceGenerator for String return type uses ProtoAdapter.STRING
            // So we just need to encode string to bytes
            try {
                // Return value wrapped in AsyncResult<String>
                // Reply packet
                return new RpcPacket.Builder()
                        .type(RpcPacket.Type.RESPONSE)
                        .requestId(packet.requestId)
                        .payload(ByteString.of(com.squareup.wire.ProtoAdapter.STRING.encode(jwt)))
                        .build();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals("mock-signed-jwt-token", jwtReceived.toString());

        // --- Step B: Call Secured Service ---
        final StringBuilder dataAck = new StringBuilder();

        MyData dataReq = new MyData();
        dataReq.setContent("Secured Content");

        dataService.processData(dataReq).thenAccept(v -> {
            dataAck.append("OK");
        });

        // Mock Server Verifying Header
        processNextPacket(mockTransport, packet -> {
            assertEquals("processData", packet.methodName);
            // VERIFY HEADER
            String authHeader = packet.headers.get("Authorization");
            assertNotNull("Authorization header missing", authHeader);
            assertEquals("Bearer mock-signed-jwt-token", authHeader);

            // Reply Void
            return new RpcPacket.Builder()
                    .type(RpcPacket.Type.RESPONSE)
                    .requestId(packet.requestId)
                    .payload(ByteString.EMPTY)
                    .build();
        });

        assertEquals("OK", dataAck.toString());
    }

    private void processNextPacket(MockTransport transport, java.util.function.Function<RpcPacket, RpcPacket> handler) {
        // In a real async environment, we'd wait. Here, RpcClient calls transport.send() synchronously?
        // No, RpcClient might be async, but TeaVM acts single-threaded unless using Promises.
        // Wait, transport.send() is called immediately.
        // So transport should have the packet in a buffer.

        byte[] data = transport.lastSentData;
        assertNotNull("No packet sent", data);
        transport.lastSentData = null; // consume

        try {
            RpcPacket packet = RpcPacket.ADAPTER.decode(data);
            RpcPacket response = handler.apply(packet);

            if (response != null) {
                // Simulate receiving response
                byte[] responseBytes = RpcPacket.ADAPTER.encode(response);
                transport.receive(responseBytes);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static class MockTransport implements Transport {
        byte[] lastSentData;
        MessageHandler handler;

        @Override
        public void send(byte[] data) {
            this.lastSentData = data;
        }

        @Override
        public void addMessageHandler(MessageHandler handler) {
            this.handler = handler;
        }

        public void receive(byte[] data) {
            if (handler != null) handler.onMessage(data);
        }
    }
}
