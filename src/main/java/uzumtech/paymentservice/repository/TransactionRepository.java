package uzumtech.paymentservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import uzumtech.paymentservice.constant.enums.TransactionType;
import uzumtech.paymentservice.entity.TransactionEntity;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    Optional<TransactionEntity> findByTransactionId(UUID transactionId);

    // История по карте, без фильтра по типу
    Page<TransactionEntity> findBySourceIdentifierOrDestinationIdentifier(
            String sourceIdentifier,
            String destinationIdentifier,
            Pageable pageable
    );

    // История по карте + фильтр по типу
    Page<TransactionEntity> findByTypeAndSourceIdentifierOrTypeAndDestinationIdentifier(
            TransactionType type1,
            String sourceIdentifier,
            TransactionType type2,
            String destinationIdentifier,
            Pageable pageable
    );
}