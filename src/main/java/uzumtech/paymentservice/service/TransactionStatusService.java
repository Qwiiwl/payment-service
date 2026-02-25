package uzumtech.paymentservice.service;

import uzumtech.paymentservice.constant.enums.TransactionType;

import java.math.BigDecimal;
import java.util.UUID;

public interface TransactionStatusService {

    UUID createPending(TransactionType type,
                       String sourceIdentifier,
                       String destinationIdentifier,
                       BigDecimal amount);

    void markFailed(UUID txId, String reason);

    void markSuccess(UUID txId);
}