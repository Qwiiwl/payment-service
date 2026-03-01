package uzumtech.paymentservice.integration;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import uzumtech.paymentservice.adapter.NotificationAdapter;
import uzumtech.paymentservice.dto.request.NotificationSendRequest;

import static org.junit.jupiter.api.Assertions.*;

class NotificationAdapterIntegrationTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "NOTIF_USER", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "NOTIF_PASS", matches = ".+")
    void sendEmail_realService_returnsNotificationId() {
        String baseUrl = "http://217.29.121.129/notification_api";
        String user = System.getenv("NOTIF_USER");
        String pass = System.getenv("NOTIF_PASS");

        RestClient client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(h -> h.setBasicAuth(user, pass))
                .build();

        NotificationAdapter adapter = new NotificationAdapter(client);

        NotificationSendRequest req = new NotificationSendRequest(
                new NotificationSendRequest.Receiver(null, "your_email@gmail.com", null),
                "EMAIL",
                "Integration test message"
        );

        long id = adapter.send(req);
        assertTrue(id > 0);
    }
}