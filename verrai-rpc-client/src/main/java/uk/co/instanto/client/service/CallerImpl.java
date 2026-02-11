package uk.co.instanto.client.service;

import java.util.function.Function;

public class CallerImpl<T> implements Caller<T> {

    private T serviceStub;
    private Class<T> serviceType;

    public CallerImpl(T serviceStub) {
        this.serviceStub = serviceStub;
    }

    public CallerImpl(Class<T> serviceType) {
        this.serviceType = serviceType;
    }

    @Override
    public <R> R call(Function<T, R> action) {
        T stub = getService();
        if (stub == null) {
            throw new RuntimeException("Service " + (serviceType != null ? serviceType.getName() : "unknown") + " is not available.");
        }
        return action.apply(stub);
    }

    @Override
    public T getService() {
        if (serviceStub == null && serviceType != null) {
            try {
                serviceStub = UnitRegistry.getInstance().getService(serviceType);
            } catch (Exception e) {
                // Service might not be ready or factory not registered yet.
                // We return null and let call() handle it or throw exception here?
                // The prompt says "connect the stubs ... when accessed".
                // If we return null, call() throws.
                // So returning null is fine for now.
            }
        }
        return serviceStub;
    }
}
