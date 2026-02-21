package uzumtech.paymentservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PhoneTopUpResponse(
        UUID transactionId,
        String fromCard,
        String phoneNumber,
        BigDecimal amount,
        String status,
        LocalDateTime createdAt
) { }
