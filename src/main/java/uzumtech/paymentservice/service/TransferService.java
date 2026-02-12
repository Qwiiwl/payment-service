package uzumtech.paymentservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uzumtech.paymentservice.dto.TransactionEvent;
import uzumtech.paymentservice.dto.TransferResponse;
import uzumtech.paymentservice.entity.CardEntity;
import uzumtech.paymentservice.entity.TransactionEntity;
import uzumtech.paymentservice.entity.enums.CardStatus;
import uzumtech.paymentservice.entity.enums.TransactionStatus;
import uzumtech.paymentservice.entity.enums.TransactionType;
import uzumtech.paymentservice.exception.CardInactiveException;
import uzumtech.paymentservice.exception.CardNotFoundException;
import uzumtech.paymentservice.exception.InsufficientFundsException;
import uzumtech.paymentservice.mapper.TransactionEventMapper;
import uzumtech.paymentservice.repository.CardRepository;
import uzumtech.paymentservice.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    private static final String TOPIC = "transactions";

    @Transactional
    public TransferResponse transfer(String fromCardNumber, String toCardNumber, BigDecimal amount) {

        //Найти карты
        CardEntity fromCard = cardRepository.findByCardNumber(fromCardNumber)
                .orElseThrow(() -> new CardNotFoundException("Source card not found"));
        CardEntity toCard = cardRepository.findByCardNumber(toCardNumber)
                .orElseThrow(() -> new CardNotFoundException("Destination card not found"));

        //Проверка активности карт
        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardInactiveException("Source card is not active");
        }
        if (toCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardInactiveException("Destination card is not active");
        }

        //Проверка баланса
        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds on source card");
        }

        //Списание и зачисление
        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        toCard.setBalance(toCard.getBalance().add(amount));
        fromCard.setUpdatedAt(LocalDateTime.now());
        toCard.setUpdatedAt(LocalDateTime.now());
        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        //Создание транзакции
        TransactionEntity transaction = TransactionEntity.builder()
                .transactionId(UUID.randomUUID())
                .type(TransactionType.TRANSFER)
                .sourceIdentifier(fromCardNumber)
                .destinationIdentifier(toCardNumber)
                .amount(amount)
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        //Создание и отправка события в Kafka через маппер
        /*

        TransactionEvent event = TransactionEventMapper.toEvent(transaction);
        kafkaTemplate.send(TOPIC, event)
                .thenAccept(result -> System.out.println("Sent transfer event to Kafka: " + event))
                .exceptionally(ex -> {
                    System.err.println("Failed to send transfer event: " + ex.getMessage());
                    return null;
                });

         */

        //Возврат TransferResponse DTO
        return TransferResponse.builder()
                .transactionId(transaction.getTransactionId())
                .fromCard(fromCardNumber)
                .toCard(toCardNumber)
                .amount(amount)
                .status(transaction.getStatus().name())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
