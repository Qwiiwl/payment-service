package uzumtech.paymentservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uzumtech.paymentservice.dto.TransferResponse;
import uzumtech.paymentservice.entity.CardEntity;
import uzumtech.paymentservice.entity.TransactionEntity;
import uzumtech.paymentservice.entity.enums.CardStatus;
import uzumtech.paymentservice.entity.enums.TransactionStatus;
import uzumtech.paymentservice.entity.enums.TransactionType;
import uzumtech.paymentservice.exception.CardInactiveException;
import uzumtech.paymentservice.exception.CardNotFoundException;
import uzumtech.paymentservice.exception.InsufficientFundsException;
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

    @Transactional
    public TransferResponse transfer(String fromCardNumber, String toCardNumber, BigDecimal amount) {

        // Создаём объект транзакции для успешного перевода
        TransactionEntity transaction = TransactionEntity.builder()
                .transactionId(UUID.randomUUID())
                .type(TransactionType.TRANSFER)
                .sourceIdentifier(fromCardNumber)
                .destinationIdentifier(toCardNumber)
                .amount(amount)
                .createdAt(LocalDateTime.now())
                .build();

        // 1️⃣ Найти карты
        CardEntity fromCard = cardRepository.findByCardNumber(fromCardNumber)
                .orElseThrow(() -> new CardNotFoundException("Source card not found"));
        CardEntity toCard = cardRepository.findByCardNumber(toCardNumber)
                .orElseThrow(() -> new CardNotFoundException("Destination card not found"));

        // 2️⃣ Проверка активности
        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardInactiveException("Source card is not active");
        }
        if (toCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardInactiveException("Destination card is not active");
        }

        // 3️⃣ Проверка баланса
        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds on source card");
        }

        // 4️⃣ Списание и зачисление
        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        toCard.setBalance(toCard.getBalance().add(amount));
        fromCard.setUpdatedAt(LocalDateTime.now());
        toCard.setUpdatedAt(LocalDateTime.now());
        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        // 5️⃣ Сохраняем успешную транзакцию
        transaction.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(transaction);

        // 6️⃣ Возвращаем TransferResponse DTO
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
