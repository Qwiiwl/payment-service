package uzumtech.paymentservice.dto.request;

public record CardConfirmRequest(
        Long userId,
        String cardNumber,
        String otpCode
) {}
