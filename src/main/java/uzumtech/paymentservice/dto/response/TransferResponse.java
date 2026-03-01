package uzumtech.paymentservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransferResponse(
        UUID transactionId,
        String fromCard,
        String toCard,
        BigDecimal amount,
        String status,
        LocalDateTime createdAt
) {}