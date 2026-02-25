package uzumtech.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record FinePaymentRequest(
        @NotBlank String fromCard,
        @NotNull Long fineId,
        @NotNull @Positive BigDecimal amount
) { }
