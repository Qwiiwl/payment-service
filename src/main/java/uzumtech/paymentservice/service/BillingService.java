package uzumtech.paymentservice.service;

import java.math.BigDecimal;

public interface BillingService {

    void transferBetweenCards(String fromCardNumber, String toCardNumber, BigDecimal amount);
}
