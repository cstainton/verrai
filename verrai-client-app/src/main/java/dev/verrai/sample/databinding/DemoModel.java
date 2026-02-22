package dev.verrai.sample.databinding;

import dev.verrai.api.Bindable;

@Bindable
public class DemoModel {
    private String name;
    private Address address;

    public DemoModel() {
        this.name = "John Doe";
        this.address = new Address("123 Main St", "Anytown");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }
}
