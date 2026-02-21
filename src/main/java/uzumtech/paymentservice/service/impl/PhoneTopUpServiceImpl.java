package uzumtech.paymentservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uzumtech.paymentservice.dto.*;
import uzumtech.paymentservice.dto.request.PhoneTopUpRequest;
import uzumtech.paymentservice.dto.response.PhoneTopUpResponse;
import uzumtech.paymentservice.entity.*;
import uzumtech.paymentservice.constant.enums.*;
import uzumtech.paymentservice.exception.*;
import uzumtech.paymentservice.repository.*;
import uzumtech.paymentservice.service.PhoneTopUpService;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhoneTopUpServiceImpl implements PhoneTopUpService {

    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    private static final String TOPIC = "transactions";

    @Override
    @Transactional
    public PhoneTopUpResponse topUp(PhoneTopUpRequest request) {

        CardEntity fromCard = cardRepository.findByCardNumber(request.fromCard())
                .orElseThrow(() -> new CardNotFoundException("Card not found"));

        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardInactiveException("Card is not active");
        }

        if (fromCard.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds on card");
        }

        fromCard.setBalance(fromCard.getBalance().subtract(request.amount()));
        fromCard.setUpdatedAt(LocalDateTime.now());
        cardRepository.save(fromCard);

        TransactionEntity transaction = TransactionEntity.builder()
                .transactionId(UUID.randomUUID())
                .type(TransactionType.PHONE_TOPUP)
                .sourceIdentifier(fromCard.getCardNumber())
                .destinationIdentifier(request.phoneNumber())
                .amount(request.amount())
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);

        return new PhoneTopUpResponse(
                transaction.getTransactionId(),
                fromCard.getCardNumber(),
                request.phoneNumber(),
                request.amount(),
                transaction.getStatus().name(),
                transaction.getCreatedAt()
        );
    }
}
