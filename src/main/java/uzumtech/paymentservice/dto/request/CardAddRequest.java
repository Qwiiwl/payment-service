package uzumtech.paymentservice.dto.request;

public record CardAddRequest(
        Long userId,
        String cardNumber
) {}
