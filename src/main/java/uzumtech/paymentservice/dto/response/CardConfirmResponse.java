package uzumtech.paymentservice.dto.response;

import java.time.LocalDateTime;

public record CardConfirmResponse(
        String cardNumber,
        String status,
        LocalDateTime createdAt
) {}
