package uzumtech.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CardConfirmRequest(
        @NotNull Long userId,
        @NotBlank String cardNumber,
        @NotNull String otpCode
) {}
