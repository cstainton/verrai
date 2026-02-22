package dev.verrai.teavm.stomp;

import org.teavm.jso.JSFunctor;

@JSFunctor
public interface ConnectCallback extends org.teavm.jso.JSObject {
    void onConnect(Frame frame);
}
