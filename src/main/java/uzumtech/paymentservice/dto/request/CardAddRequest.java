package uzumtech.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CardAddRequest(
        @NotNull Long userId,
        @NotBlank String cardNumber
) {}
