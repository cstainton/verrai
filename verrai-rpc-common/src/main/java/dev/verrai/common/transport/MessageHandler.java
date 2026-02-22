package dev.verrai.common.transport;

public interface MessageHandler {
    void onMessage(byte[] data);
}
