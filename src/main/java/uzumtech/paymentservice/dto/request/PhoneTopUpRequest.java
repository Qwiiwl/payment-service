package uzumtech.paymentservice.dto.request;

import java.math.BigDecimal;

public record PhoneTopUpRequest(
        String fromCard,
        String phoneNumber,
        BigDecimal amount
) { }
