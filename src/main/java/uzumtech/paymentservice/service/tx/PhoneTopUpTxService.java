package uzumtech.paymentservice.service.tx;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uzumtech.paymentservice.constant.enums.TransactionStatus;
import uzumtech.paymentservice.constant.enums.TransactionType;
import uzumtech.paymentservice.entity.CardEntity;
import uzumtech.paymentservice.entity.TransactionEntity;
import uzumtech.paymentservice.repository.CardRepository;
import uzumtech.paymentservice.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


 //транзакция только для сохранений пополнения телефона
@Component
@RequiredArgsConstructor
public class PhoneTopUpTxService {

    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public TransactionEntity applyTopUp(CardEntity fromCard, String phoneNumber, BigDecimal amount) {
        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        fromCard.setUpdatedAt(LocalDateTime.now());
        cardRepository.save(fromCard);

        TransactionEntity transaction = TransactionEntity.builder()
                .transactionId(UUID.randomUUID())
                .type(TransactionType.PHONE_TOPUP)
                .sourceIdentifier(fromCard.getCardNumber())
                .destinationIdentifier(phoneNumber)
                .amount(amount)
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        return transactionRepository.save(transaction);
    }
}
