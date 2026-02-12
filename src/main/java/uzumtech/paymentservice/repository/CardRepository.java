package uzumtech.paymentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uzumtech.paymentservice.entity.CardEntity;

import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<CardEntity, Long> {
    Optional<CardEntity> findByCardNumber(String cardNumber);
}
