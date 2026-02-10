package uzumtech.paymentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uzumtech.paymentservice.domain.Payment;

import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
}
