package dev.verrai.sample;

import uk.co.instanto.client.service.AsyncResult;
import uk.co.instanto.client.service.AsyncResultImpl;
import uk.co.instanto.client.service.dto.LogonRequest;
import uk.co.instanto.client.service.dto.LogonResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * <p><strong>SECURITY WARNING:</strong> This is a sample implementation for demonstration purposes.</p>
 *
 * <p>It currently employs a "dummy" authentication logic that accepts any non-empty username
 * and password. This is <strong>INSECURE</strong> and is NOT suitable for production use.</p>
 *
 * <p>In a real application, you MUST replace this with a secure implementation that
 * validates credentials against a trusted backend, database, or Identity Provider (IdP)
 * and issues cryptographically signed tokens.</p>
 */
public class AuthenticationServiceImpl implements AuthenticationService {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public AsyncResult<LogonResponse> login(LogonRequest request) {
        AsyncResultImpl<LogonResponse> result = new AsyncResultImpl<>();

        scheduler.schedule(() -> {
            String username = request.getUsername();
            String password = request.getPassword();

            // MOCK AUTHENTICATION LOGIC:
            // For this sample app, we accept any credentials as long as they are not empty.
            if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
                LogonResponse response = new LogonResponse();
                // In a real app, this would be a real signed JWT issued by an auth server
                response.setJwtToken("dummy-jwt-token-from-worker-" + username);
                response.setStompAddress("queue/secure-service");
                result.complete(response);
            } else {
                result.completeExceptionally(new RuntimeException("Authentication failed: Username and password are required."));
            }
        }, 1000, TimeUnit.MILLISECONDS);

        return result;
    }
}
