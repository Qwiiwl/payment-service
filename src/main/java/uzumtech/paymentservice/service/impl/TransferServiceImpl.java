package uzumtech.paymentservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uzumtech.paymentservice.dto.*;
import uzumtech.paymentservice.dto.response.TransferResponse;
import uzumtech.paymentservice.entity.*;
import uzumtech.paymentservice.constant.enums.*;
import uzumtech.paymentservice.exception.*;
import uzumtech.paymentservice.repository.*;
import uzumtech.paymentservice.service.TransferService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    private static final String TOPIC = "transactions";

    @Override
    @Transactional
    public TransferResponse transfer(String fromCardNumber, String toCardNumber, BigDecimal amount) {

        CardEntity fromCard = cardRepository.findByCardNumber(fromCardNumber)
                .orElseThrow(() -> new CardNotFoundException("Source card not found"));

        CardEntity toCard = cardRepository.findByCardNumber(toCardNumber)
                .orElseThrow(() -> new CardNotFoundException("Destination card not found"));

        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardInactiveException("Source card is not active");
        }

        if (toCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardInactiveException("Destination card is not active");
        }

        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds on source card");
        }

        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        toCard.setBalance(toCard.getBalance().add(amount));

        fromCard.setUpdatedAt(LocalDateTime.now());
        toCard.setUpdatedAt(LocalDateTime.now());

        cardRepository.save(fromCard);
        cardRepository.save(toCard);

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
