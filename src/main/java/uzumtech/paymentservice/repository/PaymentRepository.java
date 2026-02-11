package uzumtech.paymentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uzumtech.paymentservice.entity.PaymentEntity;

public interface PaymentRepository
        extends JpaRepository<PaymentEntity, Long> {
}
