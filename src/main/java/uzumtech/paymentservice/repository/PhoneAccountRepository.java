package uzumtech.paymentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uzumtech.paymentservice.entity.PhoneAccountEntity;

import java.util.Optional;

@Repository
public interface PhoneAccountRepository extends JpaRepository<PhoneAccountEntity, Long> {
    Optional<PhoneAccountEntity> findByPhoneNumber(String phoneNumber);
}
