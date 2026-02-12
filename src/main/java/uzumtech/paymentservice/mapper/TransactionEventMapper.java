package uzumtech.paymentservice.mapper;

import uzumtech.paymentservice.dto.TransactionEvent;
import uzumtech.paymentservice.entity.TransactionEntity;

public class TransactionEventMapper {

    private TransactionEventMapper() {
        // приватный конструктор, чтобы нельзя было создавать объект
    }

    public static TransactionEvent toEvent(TransactionEntity transaction) {
        return new TransactionEvent(
                transaction.getTransactionId(),
                transaction.getType().name(),
                transaction.getSourceIdentifier(),
                transaction.getDestinationIdentifier(),
                transaction.getAmount(),
                transaction.getStatus().name(),
                transaction.getCreatedAt()
        );
    }
}
