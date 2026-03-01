package uzumtech.paymentservice.dto.request;

public record NotificationSendRequest(
        Receiver receiver,
        String type,
        String text
) {
    public record Receiver(String phone, String email, String firebaseToken) {}
}