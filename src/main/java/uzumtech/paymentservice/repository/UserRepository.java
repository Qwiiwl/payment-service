package uzumtech.paymentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uzumtech.paymentservice.entity.UserEntity;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);
}