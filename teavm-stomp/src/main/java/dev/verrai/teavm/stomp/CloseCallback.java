package dev.verrai.teavm.stomp;

import org.teavm.jso.JSFunctor;

@JSFunctor
public interface CloseCallback extends org.teavm.jso.JSObject {
    void onClose();
}
