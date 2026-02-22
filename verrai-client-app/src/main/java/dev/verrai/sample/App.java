package dev.verrai.sample;

import dev.verrai.api.EntryPoint;
import jakarta.annotation.PostConstruct;
import dev.verrai.api.Navigation;
import jakarta.inject.Inject;

@EntryPoint
public class App {

    @Inject
    public Navigation navigation;

    @PostConstruct
    public void onModuleLoad() {
        navigation.start();
    }

    public static void main(String[] args) {
        // new BootstrapperImpl().run(); // Requires new DI bootstrapper
    }
}
