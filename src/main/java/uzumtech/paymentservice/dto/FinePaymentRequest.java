package uzumtech.paymentservice.dto;

import java.math.BigDecimal;

public class FinePaymentRequest {

    private String fromCard;
    private Long fineId; // ID штрафа
    private BigDecimal amount;

    public FinePaymentRequest() { }

    public FinePaymentRequest(String fromCard, Long fineId, BigDecimal amount) {
        this.fromCard = fromCard;
        this.fineId = fineId;
        this.amount = amount;
    }

    public String getFromCard() { return fromCard; }
    public void setFromCard(String fromCard) { this.fromCard = fromCard; }

    public Long getFineId() { return fineId; }
    public void setFineId(Long fineId) { this.fineId = fineId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
