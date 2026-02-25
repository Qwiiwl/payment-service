package uzumtech.paymentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uzumtech.paymentservice.constant.enums.OtpStatus;
import uzumtech.paymentservice.entity.OtpEntity;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpEntity, Long> {

    Optional<OtpEntity> findTopByUserIdAndCardNumberAndStatusOrderByCreatedAtDesc(
            Long userId,
            String cardNumber,
            OtpStatus status
    );
}