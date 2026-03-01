package uzumtech.paymentservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uzumtech.paymentservice.constant.enums.TransactionType;
import uzumtech.paymentservice.entity.TransactionEntity;
import uzumtech.paymentservice.mapper.TransactionMapper;
import uzumtech.paymentservice.repository.TransactionRepository;
import uzumtech.paymentservice.service.TransactionStatusService;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionStatusServiceImpl implements TransactionStatusService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Override
    public UUID createPending(TransactionType type,
                              String sourceIdentifier,
                              String destinationIdentifier,
                              BigDecimal amount) {

        TransactionEntity entity = transactionMapper.createPending(
                type, sourceIdentifier, destinationIdentifier, amount
        );

        return transactionRepository.save(entity).getTransactionId();
    }

    @Override
    public void markFailed(UUID txId, String reason) {
        TransactionEntity entity = transactionRepository.findByTransactionId(txId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + txId));

        transactionMapper.markFailed(entity, reason);
        transactionRepository.save(entity);
    }

    @Override
    public void markSuccess(UUID txId) {
        TransactionEntity entity = transactionRepository.findByTransactionId(txId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + txId));

        transactionMapper.markSuccess(entity);
        transactionRepository.save(entity);
    }
}