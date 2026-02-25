package uzumtech.paymentservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import uzumtech.paymentservice.adapter.NotificationAdapter;

@SpringBootTest(
        classes = PaymentServiceApplicationTests.TestConfig.class,
        properties = {
                "spring.main.allow-bean-definition-overriding=true",

                "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",

                // чтобы не стартовали kafka listeners
                "spring.kafka.listener.auto-startup=false",

                // чтобы не требовались notification.* проперти
                "notification.base-url=http://localhost:9999",
                "notification.username=test",
                "notification.password=test",
                "notification.timeout-ms=1000"
        }
)
class PaymentServiceApplicationTests {

    @Configuration
    @EnableAutoConfiguration
    static class TestConfig {
        //просто включаем автоконфиги Spring Boot
    }

    // подменяем адаптер (внешний вызов)
    @MockBean
    NotificationAdapter notificationAdapter;

    @Test
    void contextLoads() {
    }
}