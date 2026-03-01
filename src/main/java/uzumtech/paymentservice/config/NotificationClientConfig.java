package uzumtech.paymentservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class NotificationClientConfig {

    @Bean
    public RestClient notificationRestClient(
            @Value("${notification.base-url:http://217.29.121.129/notification_api}") String baseUrl,
            @Value("${notification.username:}") String username,
            @Value("${notification.password:}") String password,
            @Value("${notification.timeout-ms:5000}") long timeoutMs
    ) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) timeoutMs);
        factory.setReadTimeout((int) timeoutMs);

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory);

        // Подключаем BasicAuth
        if (username != null && !username.isBlank()
                && password != null && !password.isBlank()) {

            builder.defaultHeaders(headers ->
                    headers.setBasicAuth(username, password)
            );
        }

        return builder.build();
    }
}