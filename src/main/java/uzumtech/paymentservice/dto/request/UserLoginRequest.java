package uzumtech.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserLoginRequest(
        @NotBlank String phoneNumber,
        @NotBlank String password
) {}