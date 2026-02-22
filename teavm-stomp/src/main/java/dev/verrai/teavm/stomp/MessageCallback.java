package dev.verrai.teavm.stomp;

import org.teavm.jso.JSFunctor;

@JSFunctor
public interface MessageCallback extends org.teavm.jso.JSObject {
    void onMessage(Message message);
}
