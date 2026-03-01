package uzumtech.paymentservice.service.tx;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uzumtech.paymentservice.constant.enums.TransactionStatus;
import uzumtech.paymentservice.constant.enums.TransactionType;
import uzumtech.paymentservice.entity.CardEntity;
import uzumtech.paymentservice.entity.FineEntity;
import uzumtech.paymentservice.entity.TransactionEntity;
import uzumtech.paymentservice.repository.CardRepository;
import uzumtech.paymentservice.repository.FineRepository;
import uzumtech.paymentservice.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


 //транзакция только для сохранений по оплате штрафа
@Component
@RequiredArgsConstructor
public class FinePaymentTxService {

    private final CardRepository cardRepository;
    private final FineRepository fineRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public TransactionEntity applyFinePayment(CardEntity fromCard, FineEntity fine, BigDecimal amount) {
        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        fromCard.setUpdatedAt(LocalDateTime.now());
        cardRepository.save(fromCard);

        fine.setPaid(true);
        fine.setPaidAt(LocalDateTime.now());
        fineRepository.save(fine);

        TransactionEntity transaction = TransactionEntity.builder()
                .transactionId(UUID.randomUUID())
                .type(TransactionType.FINE_PAYMENT)
                .sourceIdentifier(fromCard.getCardNumber())
                .destinationIdentifier("FINE#" + fine.getId())
                .amount(amount)
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        return transactionRepository.save(transaction);
    }
}
