package uzumtech.paymentservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import uzumtech.paymentservice.domain.PaymentType;

import java.math.BigDecimal;

@Data
public class PaymentRequestDto {

    @NotNull
    private PaymentType type;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private String currency;

    @NotNull
    private String account;
}
