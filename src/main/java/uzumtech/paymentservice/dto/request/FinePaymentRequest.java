package uzumtech.paymentservice.dto.request;

import java.math.BigDecimal;

public record FinePaymentRequest(
        String fromCard,
        Long fineId,
        BigDecimal amount
) { }
