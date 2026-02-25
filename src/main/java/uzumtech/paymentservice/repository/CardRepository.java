package uzumtech.paymentservice.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import uzumtech.paymentservice.entity.CardEntity;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface CardRepository extends JpaRepository<CardEntity, Long> {

    Optional<CardEntity> findByCardNumber(String cardNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CardEntity c where c.cardNumber = :cardNumber")
    Optional<CardEntity> findByCardNumberForUpdate(@Param("cardNumber") String cardNumber);

    boolean existsByCardNumber(String cardNumber);
}