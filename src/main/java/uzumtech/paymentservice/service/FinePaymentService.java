package uzumtech.paymentservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uzumtech.paymentservice.dto.FinePaymentRequest;
import uzumtech.paymentservice.dto.FinePaymentResponse;
import uzumtech.paymentservice.dto.TransactionEvent;
import uzumtech.paymentservice.entity.CardEntity;
import uzumtech.paymentservice.entity.FineEntity;
import uzumtech.paymentservice.entity.TransactionEntity;
import uzumtech.paymentservice.entity.enums.CardStatus;
import uzumtech.paymentservice.entity.enums.TransactionStatus;
import uzumtech.paymentservice.entity.enums.TransactionType;
import uzumtech.paymentservice.exception.*;
import uzumtech.paymentservice.mapper.TransactionEventMapper;
import uzumtech.paymentservice.repository.CardRepository;
import uzumtech.paymentservice.repository.FineRepository;
import uzumtech.paymentservice.repository.TransactionRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FinePaymentService {

    private final CardRepository cardRepository;
    private final FineRepository fineRepository;
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    private static final String TOPIC = "transactions";

    @Transactional
    public FinePaymentResponse payFine(FinePaymentRequest request) {

        //Найти карту
        CardEntity fromCard = cardRepository.findByCardNumber(request.getFromCard())
                .orElseThrow(() -> new CardNotFoundException("Card not found"));

        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardInactiveException("Card is not active");
        }

        //Найти штраф
        FineEntity fine = fineRepository.findById(request.getFineId())
                .orElseThrow(() -> new FineNotFoundException("Fine not found"));

        //Проверка, что штраф не оплачен
        if (Boolean.TRUE.equals(fine.getPaid())) {
            throw new FineAlreadyPaidException("Fine already paid");
        }

        //Проверка суммы
        if (request.getAmount() == null || request.getAmount().compareTo(fine.getAmount()) != 0) {
            throw new IllegalArgumentException("Amount must equal fine amount");
        }

        //Проверка баланса
        if (fromCard.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds on card");
        }

        //Списание средств
        fromCard.setBalance(fromCard.getBalance().subtract(request.getAmount()));
        fromCard.setUpdatedAt(LocalDateTime.now());
        cardRepository.save(fromCard);

        //Обновление штрафа
        fine.setPaid(true);
        fine.setPaidAt(LocalDateTime.now());
        fineRepository.save(fine);

        //Создание транзакции
        TransactionEntity transaction = TransactionEntity.builder()
                .transactionId(UUID.randomUUID())
                .type(TransactionType.FINE_PAYMENT)
                .sourceIdentifier(fromCard.getCardNumber())
                .destinationIdentifier("FINE#" + fine.getId())
                .amount(request.getAmount())
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        //Отправка события в Kafka
        //TransactionEvent event = TransactionEventMapper.toEvent(transaction);
        //kafkaTemplate.send(TOPIC, event);

        //Возврат DTO
        return new FinePaymentResponse(
                transaction.getTransactionId(),
                fromCard.getCardNumber(),
                fine.getFineNumber(),
                request.getAmount(),
                transaction.getStatus().name(),
                transaction.getCreatedAt()
        );
    }
}
