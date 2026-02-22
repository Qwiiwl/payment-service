package uzumtech.paymentservice.dto.request;

public record UserRegisterRequest(
        String fullName,
        String phoneNumber,
        String password
) {}