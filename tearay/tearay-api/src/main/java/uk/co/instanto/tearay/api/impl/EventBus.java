package uk.co.instanto.tearay.api.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EventBus {
    private static EventBus instance;
    private final Map<Class<?>, List<Consumer<Object>>> subscribers = new HashMap<>();

    public static EventBus getInstance() {
        if (instance == null) {
            instance = new EventBus();
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    public <T> Runnable subscribe(Class<T> type, Consumer<T> listener) {
        List<Consumer<Object>> list = subscribers.computeIfAbsent(type, k -> new ArrayList<>());
        Consumer<Object> castListener = (Consumer<Object>) listener;
        list.add(castListener);

        return () -> list.remove(castListener);
    }

    public void fire(Object event) {
        if (event == null) return;
        List<Consumer<Object>> listeners = subscribers.get(event.getClass());
        if (listeners != null) {
            // Copy to avoid concurrent modification if listener unsubscribes during fire
            for (Consumer<Object> listener : new ArrayList<>(listeners)) {
                listener.accept(event);
            }
        }
    }
}
