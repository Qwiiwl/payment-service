package uzumtech.paymentservice.integration;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;


@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("payment_db")
                    .withUsername("arvi")
                    .withPassword("123456");

    @BeforeAll
    static void start() {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);

        // чтобы Hibernate сам создал таблицы под тесты
        r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        // чтобы в тестах не зависеть от kafka
        r.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
    }
}