package uzumtech.paymentservice.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import uzumtech.paymentservice.dto.response.NotificationSendResponse;
import uzumtech.paymentservice.dto.request.NotificationSendRequest;

@Component
@RequiredArgsConstructor
public class NotificationAdapter {

    private final RestClient notificationRestClient;

    public long send(NotificationSendRequest request) {
        NotificationSendResponse response = notificationRestClient.post()
                .uri("/api/notifications/sending")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(NotificationSendResponse.class);

        if (response == null || response.data() == null) {
            throw new IllegalStateException("Notification service returned empty response");
        }

        return response.data().notificationId();
    }
}