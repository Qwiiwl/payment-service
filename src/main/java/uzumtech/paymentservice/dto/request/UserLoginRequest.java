package uzumtech.paymentservice.dto.request;

public record UserLoginRequest(
        String phoneNumber,
        String password
) {}