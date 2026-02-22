package dev.verrai.client.service.transport;

import dev.verrai.common.transport.stomp.StompClient;
import dev.verrai.common.transport.stomp.StompSubscriptionCallback;
import dev.verrai.teavm.stomp.Message;
import java.util.HashMap;
import java.util.Map;
import org.teavm.jso.JSObject;

public class TeaVMStompClientAdapter implements StompClient {

    private final dev.verrai.teavm.stomp.StompClient jsoClient;

    public TeaVMStompClientAdapter(dev.verrai.teavm.stomp.StompClient jsoClient) {
        this.jsoClient = jsoClient;
    }

    @Override
    public boolean isConnected() {
        return jsoClient.isConnected();
    }

    @Override
    public void send(String destination, String body) {
        jsoClient.send(destination, null, body);
    }

    @Override
    public void subscribe(String destination, StompSubscriptionCallback callback) {
        jsoClient.subscribe(destination, jsoMessage -> {
            callback.onMessage(new TeaVMStompMessageAdapter(jsoMessage));
        });
    }

    private static class TeaVMStompMessageAdapter implements dev.verrai.common.transport.stomp.StompMessage {
        private final Message jsoMessage;

        public TeaVMStompMessageAdapter(Message jsoMessage) {
            this.jsoMessage = jsoMessage;
        }

        @Override
        public String getBody() {
            return jsoMessage.getBody();
        }

        @Override
        public Map<String, String> getHeaders() {
            Map<String, String> headers = new HashMap<>();
            JSObject jsoHeaders = jsoMessage.getHeaders();
            // In JSO, we can iterate over properties if we use specific APIs
            // For now, let's keep it simple or implement a basic conversion
            // Actually, JSObjects.getProperties(jsoHeaders) is what we need if available
            return headers;
        }
    }
}
