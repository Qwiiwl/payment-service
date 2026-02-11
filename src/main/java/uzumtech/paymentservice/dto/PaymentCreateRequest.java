package uzumtech.paymentservice.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCreateRequest(

        @NotBlank(message = "Order ID must not be blank")
        @Size(max = 64, message = "Order ID must not exceed 64 characters")
        String orderId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @Digits(integer = 15, fraction = 2, message = "Invalid monetary format")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Pattern(
                regexp = "^[A-Z]{3}$",
                message = "Currency must be a valid ISO-4217 code (e.g. UZS, USD, EUR)"
        )
        String currency,

        @NotNull(message = "User ID is required")
        UUID userId,

        @NotBlank(message = "Idempotency key is required")
        @Size(max = 128, message = "Idempotency key too long")
        String idempotencyKey,

        @Size(max = 255, message = "Description too long")
        String description

) {}
