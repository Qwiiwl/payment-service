package uzumtech.paymentservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class FinePaymentResponse {

    private UUID transactionId;
    private String fromCard;
    private String fineNumber;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;

    public FinePaymentResponse(UUID transactionId, String fromCard, String fineNumber,
                               BigDecimal amount, String status, LocalDateTime createdAt) {
        this.transactionId = transactionId;
        this.fromCard = fromCard;
        this.fineNumber = fineNumber;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    //Геттеры
    public UUID getTransactionId() { return transactionId; }
    public String getFromCard() { return fromCard; }
    public String getFineNumber() { return fineNumber; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
