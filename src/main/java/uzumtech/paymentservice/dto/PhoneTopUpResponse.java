package uzumtech.paymentservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PhoneTopUpResponse {

    private UUID transactionId;
    private String fromCard;
    private String phoneNumber;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;

    public PhoneTopUpResponse(UUID transactionId, String fromCard, String phoneNumber, BigDecimal amount, String status, LocalDateTime createdAt) {
        this.transactionId = transactionId;
        this.fromCard = fromCard;
        this.phoneNumber = phoneNumber;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    // геттеры
    public UUID getTransactionId() { return transactionId; }
    public String getFromCard() { return fromCard; }
    public String getPhoneNumber() { return phoneNumber; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
