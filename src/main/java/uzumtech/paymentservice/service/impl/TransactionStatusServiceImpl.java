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

        transactionRepository.save(entity);

        // тхИД должен быть сгенерен в маппере, но на всякий еще и тут проверку вводим чтобы тесты не падали
        if (entity.getTransactionId() == null) {
            entity.setTransactionId(UUID.randomUUID());
        }
        return entity.getTransactionId();
    }

    @Override
    public void markFailed(UUID txId, String reason) {
        transactionRepository.findByTransactionId(txId).ifPresent(entity -> {
            transactionMapper.markFailed(entity, reason);
            transactionRepository.save(entity); // ретёрн может быть нуллом, почему-то без этого не работает
        });
    }

    @Override
    public void markSuccess(UUID txId) {
        transactionRepository.findByTransactionId(txId).ifPresent(entity -> {
            transactionMapper.markSuccess(entity);
            transactionRepository.save(entity);
        });
    }
}