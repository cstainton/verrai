package dev.verrai.common.transport.stomp;

public interface StompSubscriptionCallback {
    void onMessage(StompMessage message);
}
