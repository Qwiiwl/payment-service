package uzumtech.paymentservice.dto;

import java.math.BigDecimal;

public class TransferRequest {

    private String fromCard;
    private String toCard;
    private BigDecimal amount;

    public String getFromCard() {
        return fromCard;
    }

    public void setFromCard(String fromCard) {
        this.fromCard = fromCard;
    }

    public String getToCard() {
        return toCard;
    }

    public void setToCard(String toCard) {
        this.toCard = toCard;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
