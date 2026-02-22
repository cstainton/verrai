package dev.verrai.sample;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HelloService implements GreetingService {
    @Override
    public String getGreeting() {
        return "Hello from Injected Service! Time: " + System.currentTimeMillis();
    }
}
