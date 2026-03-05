package uzumtech.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CardConfirmRequest(
        @NotNull Long userId,
        @NotBlank @Pattern(regexp = "\\d{16}", message = "Card number must be exactly 16 digits")
        String cardNumber,
        @NotBlank String otpCode
) {}
