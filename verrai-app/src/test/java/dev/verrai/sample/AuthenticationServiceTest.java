package dev.verrai.sample;

import org.junit.Test;
import uk.co.instanto.client.service.dto.LogonRequest;
import uk.co.instanto.client.service.dto.LogonResponse;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class AuthenticationServiceTest {

    @Test
    public void testSuccessfulLogin() throws InterruptedException {
        AuthenticationServiceImpl authService = new AuthenticationServiceImpl();
        LogonRequest request = new LogonRequest("AnyUser", "AnyPassword");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<LogonResponse> responseRef = new AtomicReference<>();

        authService.login(request).thenAccept(response -> {
            responseRef.set(response);
            latch.countDown();
        });

        assertTrue("Login timed out", latch.await(2, TimeUnit.SECONDS));
        assertNotNull(responseRef.get());
        assertEquals("dummy-jwt-token-from-worker-AnyUser", responseRef.get().getJwtToken());
    }

    @Test
    public void testEmptyUsernameFails() throws InterruptedException {
        AuthenticationServiceImpl authService = new AuthenticationServiceImpl();
        LogonRequest request = new LogonRequest("", "some-password");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        authService.login(request).exceptionally(err -> {
            errorRef.set(err);
            latch.countDown();
            return null;
        });

        assertTrue("Login failure timed out", latch.await(2, TimeUnit.SECONDS));
        assertNotNull(errorRef.get());
        assertTrue(errorRef.get().getMessage().contains("Username and password are required"));
    }

    @Test
    public void testEmptyPasswordFails() throws InterruptedException {
        AuthenticationServiceImpl authService = new AuthenticationServiceImpl();
        LogonRequest request = new LogonRequest("user", "");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        authService.login(request).exceptionally(err -> {
            errorRef.set(err);
            latch.countDown();
            return null;
        });

        assertTrue("Login failure timed out", latch.await(2, TimeUnit.SECONDS));
        assertNotNull(errorRef.get());
        assertTrue(errorRef.get().getMessage().contains("Username and password are required"));
    }
}
