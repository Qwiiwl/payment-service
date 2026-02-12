package uzumtech.paymentservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonFormat;
@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
public class TransactionEvent {

    private UUID transactionId;
    private String transactionType; // FINE_PAYMENT, PHONE_TOP_UP, TRANSFER
    private String source;
    private String destination;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;

    public TransactionEvent() {}

    public TransactionEvent(UUID transactionId, String transactionType, String source,
                            String destination, BigDecimal amount, String status, LocalDateTime createdAt) {
        this.transactionId = transactionId;
        this.transactionType = transactionType;
        this.source = source;
        this.destination = destination;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Геттеры и сеттеры
    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }


}
