package uzumtech.paymentservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uzumtech.paymentservice.constant.enums.TransactionType;
import uzumtech.paymentservice.dto.response.TransactionHistoryResponse;
import uzumtech.paymentservice.entity.TransactionEntity;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface TransactionMapper {


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "transactionId", expression = "java(UUID.randomUUID())")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(LocalDateTime.now())")
    TransactionEntity createPending(
            TransactionType type,
            String sourceIdentifier,
            String destinationIdentifier,
            BigDecimal amount
    );


    @Mapping(target = "transactionId", source = "transactionId")
    @Mapping(target = "type",
            expression = "java(entity.getType() == null ? null : entity.getType().name())")
    @Mapping(target = "sourceIdentifier", source = "sourceIdentifier")
    @Mapping(target = "destinationIdentifier", source = "destinationIdentifier")
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "status",
            expression = "java(entity.getStatus() == null ? null : entity.getStatus().name())")
    @Mapping(target = "errorMessage", source = "errorMessage")
    @Mapping(target = "createdAt", source = "createdAt")
    TransactionHistoryResponse toHistoryResponse(TransactionEntity entity);

    default void markSuccess(TransactionEntity entity) {
        entity.setStatus(uzumtech.paymentservice.constant.enums.TransactionStatus.SUCCESS);
        entity.setErrorMessage(null);
        entity.setUpdatedAt(LocalDateTime.now());
    }

    default void markFailed(TransactionEntity entity, String reason) {
        entity.setStatus(uzumtech.paymentservice.constant.enums.TransactionStatus.FAILED);
        entity.setErrorMessage(reason);
        entity.setUpdatedAt(LocalDateTime.now());
    }
}