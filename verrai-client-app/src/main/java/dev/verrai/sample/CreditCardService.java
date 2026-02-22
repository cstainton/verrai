package dev.verrai.sample;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

@Dependent
@Named("Credit")
public class CreditCardService implements PaymentService {
    @Override
    public String pay(int amount) {
        return "Paid " + amount + " via Credit Card";
    }
}
