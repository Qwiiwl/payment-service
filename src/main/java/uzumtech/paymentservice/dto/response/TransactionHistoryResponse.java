package uzumtech.paymentservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionHistoryResponse(
        UUID transactionId,
        String type,
        String sourceIdentifier,
        String destinationIdentifier,
        BigDecimal amount,
        String status,
        String errorMessage,
        LocalDateTime createdAt
) {}