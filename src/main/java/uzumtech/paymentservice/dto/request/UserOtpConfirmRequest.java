package uzumtech.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserOtpConfirmRequest(
        @NotBlank String phoneNumber,
        @NotBlank String code
) {}