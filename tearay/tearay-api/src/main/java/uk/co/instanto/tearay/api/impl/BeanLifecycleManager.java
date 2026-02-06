package uk.co.instanto.tearay.api.impl;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class BeanLifecycleManager {
    private static BeanLifecycleManager instance;
    private final Map<Object, List<Runnable>> disposables = new IdentityHashMap<>();

    public static BeanLifecycleManager getInstance() {
        if (instance == null) {
            instance = new BeanLifecycleManager();
        }
        return instance;
    }

    public void register(Object bean, Runnable disposable) {
        disposables.computeIfAbsent(bean, k -> new ArrayList<>()).add(disposable);
    }

    public void destroy(Object bean) {
        List<Runnable> list = disposables.remove(bean);
        if (list != null) {
            for (Runnable runnable : list) {
                runnable.run();
            }
        }
    }
}
