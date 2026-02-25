package uzumtech.paymentservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransferResponse(
        UUID transactionId,       // Уникальный идентификатор транзакции
        String fromCard,          // Номер карты отправителя
        String toCard,            // Номер карты получателя
        BigDecimal amount,        // Сумма перевода
        String status,            // Статус перевода: SUCCESS / FAILED
        LocalDateTime createdAt   // Время создания транзакции
) {}