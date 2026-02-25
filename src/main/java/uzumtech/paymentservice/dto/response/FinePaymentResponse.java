package uzumtech.paymentservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record FinePaymentResponse(
        UUID transactionId,
        String fromCard,
        String fineNumber,
        BigDecimal amount,
        String status,
        LocalDateTime createdAt
) { }
