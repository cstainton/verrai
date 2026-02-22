package dev.verrai.sample;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

@Dependent
@Named("Debit")
public class DebitCardService implements PaymentService {
    @Override
    public String pay(int amount) {
        return "Paid " + amount + " via Debit Card";
    }
}
