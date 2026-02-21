package uzumtech.paymentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {
    private UUID transactionId;       // Уникальный идентификатор транзакции
    private String fromCard;          // Номер карты отправителя
    private String toCard;            // Номер карты получателя
    private BigDecimal amount;        // Сумма перевода
    private String status;            // Статус перевода: SUCCESS / FAILED
    private LocalDateTime createdAt;  // Время создания транзакции
}
