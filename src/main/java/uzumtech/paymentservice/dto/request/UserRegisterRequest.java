package uzumtech.paymentservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRegisterRequest(
        @NotBlank String fullName,
        @NotBlank String phoneNumber,
        @NotBlank String password,
        @NotBlank @Email String email
) {}