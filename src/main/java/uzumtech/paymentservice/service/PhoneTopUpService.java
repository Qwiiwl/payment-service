package uzumtech.paymentservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uzumtech.paymentservice.dto.PhoneTopUpRequest;
import uzumtech.paymentservice.dto.PhoneTopUpResponse;
import uzumtech.paymentservice.dto.TransactionEvent;
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

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhoneTopUpService {

    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    private static final String TOPIC = "transactions";

    @Transactional
    public PhoneTopUpResponse topUp(PhoneTopUpRequest request) {

        //Найти карту
        CardEntity fromCard = cardRepository.findByCardNumber(request.getFromCard())
                .orElseThrow(() -> new CardNotFoundException("Card not found"));

        //Проверка активности карты
        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardInactiveException("Card is not active");
        }

        //Проверка баланса
        if (fromCard.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds on card");
        }

        //Списание средств
        fromCard.setBalance(fromCard.getBalance().subtract(request.getAmount()));
        fromCard.setUpdatedAt(LocalDateTime.now());
        cardRepository.save(fromCard);

        //Создание транзакции
        TransactionEntity transaction = TransactionEntity.builder()
                .transactionId(UUID.randomUUID())
                .type(TransactionType.PHONE_TOPUP)
                .sourceIdentifier(fromCard.getCardNumber())
                .destinationIdentifier(request.getPhoneNumber())
                .amount(request.getAmount())
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);

        //Отправка события в Kafka
        /*


        TransactionEvent event = TransactionEventMapper.toEvent(transaction);
        kafkaTemplate.send(TOPIC, event)
                .thenAccept(result -> System.out.println("Sent phone top-up event to Kafka: " + event))
                .exceptionally(ex -> {
                    System.err.println("Failed to send phone top-up event: " + ex.getMessage());
                    return null;
                });

         */

        //Возврат DTO
        return new PhoneTopUpResponse(
                transaction.getTransactionId(),
                fromCard.getCardNumber(),
                request.getPhoneNumber(),
                request.getAmount(),
                transaction.getStatus().name(),
                transaction.getCreatedAt()
        );
    }
}
