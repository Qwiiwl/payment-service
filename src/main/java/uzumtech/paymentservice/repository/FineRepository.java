package uzumtech.paymentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uzumtech.paymentservice.entity.FineEntity;

import java.util.Optional;

@Repository
public interface FineRepository extends JpaRepository<FineEntity, Long> {
    Optional<FineEntity> findByFineNumber(String fineNumber);
}
