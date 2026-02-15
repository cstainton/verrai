package dev.verrai.sample;

import uk.co.instanto.client.service.UnitRegistry;
import uk.co.instanto.client.service.ServiceFactory;
import uk.co.instanto.client.service.dto.LogonRequest;
import dev.verrai.rpc.common.transport.Transport;
import dev.verrai.rpc.common.transport.MessageHandler;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class JreApp {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting JRE Client App...");

        // 1. Setup Transport (Mock for JRE)
        Transport jreTransport = new Transport() {
            @Override
            public void send(byte[] data) {
                System.out.println("JRE Transport SEND: " + data.length + " bytes");
                // In a real app, send to WebSocket/Socket

                // For demo, we just simulate a response locally if needed, or just log.
                // We don't have a server running, so this request will timeout or just stay pending.
            }

            @Override
            public void addMessageHandler(MessageHandler handler) {
                // No-op for now unless we mock receiving
            }
        };

        // 2. Register Remote Service
        UnitRegistry.getInstance().registerRemote(AuthenticationService.class.getName(), "auth-server", jreTransport);

        // 3. Register Factory manually (since we don't have Bootstrapper running)
        // Note: Annotation Processor generates Factory, we just need to use it.
        try {
            Class<?> factoryClass = Class.forName("dev.verrai.sample.AuthenticationService_Factory");
            Object factory = factoryClass.newInstance();
            UnitRegistry.getInstance().registerFactory(AuthenticationService.class, (ServiceFactory) factory);
            System.out.println("Factory registered: " + factoryClass.getName());
        } catch (Exception e) {
            System.err.println("Failed to load generated factory: " + e);
            e.printStackTrace();
            return;
        }

        // 4. Use the Service
        AuthenticationService authService = UnitRegistry.getInstance().getService(AuthenticationService.class);
        if (authService != null) {
            System.out.println("Service proxy created: " + authService.getClass().getName());

            LogonRequest req = new LogonRequest("JreUser", "password");
            System.out.println("Calling login()...");

            CountDownLatch latch = new CountDownLatch(1);

            authService.login(req).thenAccept(response -> {
                System.out.println("Login Successful! Token: " + response.getJwtToken());
                latch.countDown();
            }).exceptionally(err -> {
                System.err.println("Login Failed: " + err.getMessage());
                latch.countDown();
                return null;
            });

            // Wait a bit to show we sent the request
            latch.await(2, TimeUnit.SECONDS);
            System.out.println("Done waiting (Request sent, no response expected in mock mode).");
        } else {
            System.err.println("Could not get service instance.");
        }
    }
}
