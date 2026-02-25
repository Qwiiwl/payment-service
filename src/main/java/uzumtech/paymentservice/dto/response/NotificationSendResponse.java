package uzumtech.paymentservice.dto.response;

public record NotificationSendResponse(Data data) {
    public record Data(long notificationId) {}
}
