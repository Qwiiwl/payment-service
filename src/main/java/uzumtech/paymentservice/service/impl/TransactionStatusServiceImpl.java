package uzumtech.paymentservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uzumtech.paymentservice.constant.enums.TransactionStatus;
import uzumtech.paymentservice.constant.enums.TransactionType;
import uzumtech.paymentservice.entity.TransactionEntity;
import uzumtech.paymentservice.repository.TransactionRepository;
import uzumtech.paymentservice.service.TransactionStatusService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionStatusServiceImpl implements TransactionStatusService {

    private final TransactionRepository transactionRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID createPending(TransactionType type,
                              String sourceIdentifier,
                              String destinationIdentifier,
                              BigDecimal amount) {

        UUID txId = UUID.randomUUID();

        TransactionEntity tx = TransactionEntity.builder()
                .transactionId(txId)
                .type(type)
                .sourceIdentifier(sourceIdentifier)
                .destinationIdentifier(destinationIdentifier)
                .amount(amount)
                .status(TransactionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        transactionRepository.save(tx);
        return txId; //тут транзакция завершится и INSERT будет виден всем
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID txId, String reason) {
        transactionRepository.findByTransactionId(txId).ifPresent(tx -> {
            tx.setStatus(TransactionStatus.FAILED);
            tx.setErrorMessage(reason);
            tx.setUpdatedAt(LocalDateTime.now());
            transactionRepository.save(tx);
        });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(UUID txId) {
        transactionRepository.findByTransactionId(txId).ifPresent(tx -> {
            tx.setStatus(TransactionStatus.SUCCESS);
            tx.setErrorMessage(null);
            tx.setUpdatedAt(LocalDateTime.now());
            transactionRepository.save(tx);
        });
    }
}