package dev.verrai.teavm.stomp;

import org.teavm.jso.JSFunctor;

@JSFunctor
public interface DisconnectCallback extends org.teavm.jso.JSObject {
    void onDisconnect();
}
