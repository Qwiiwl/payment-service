package uzumtech.paymentservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uzumtech.paymentservice.constant.enums.CardStatus;
import uzumtech.paymentservice.constant.enums.TransactionType;
import uzumtech.paymentservice.dto.response.TransferResponse;
import uzumtech.paymentservice.entity.CardEntity;
import uzumtech.paymentservice.exception.CardInactiveException;
import uzumtech.paymentservice.exception.CardNotFoundException;
import uzumtech.paymentservice.exception.InsufficientFundsException;
import uzumtech.paymentservice.repository.CardRepository;
import uzumtech.paymentservice.service.TransferService;
import uzumtech.paymentservice.service.TransactionStatusService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final CardRepository cardRepository;
    private final TransactionStatusService transactionStatusService;

    @Override
    @Transactional
    public TransferResponse transfer(String fromCardNumber, String toCardNumber, BigDecimal amount) {

        // 0) базовая валидация
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (fromCardNumber == null || toCardNumber == null) {
            throw new IllegalArgumentException("Card numbers must not be null");
        }
        if (fromCardNumber.equals(toCardNumber)) {
            throw new IllegalArgumentException("fromCard and toCard must be different");
        }

        // ✅ 1) Создаем PENDING В ОТДЕЛЬНОЙ транзакции (REQUIRES_NEW) — чтобы запись точно попала в БД
        UUID txId = transactionStatusService.createPending(
                TransactionType.TRANSFER,
                fromCardNumber,
                toCardNumber,
                amount
        );

        try {
            // 2) блокируем обе карты в одном порядке (anti-deadlock)
            String first = fromCardNumber.compareTo(toCardNumber) <= 0 ? fromCardNumber : toCardNumber;
            String second = first.equals(fromCardNumber) ? toCardNumber : fromCardNumber;

            CardEntity firstCard = cardRepository.findByCardNumberForUpdate(first)
                    .orElseThrow(() -> new CardNotFoundException("Card not found: " + first));
            CardEntity secondCard = cardRepository.findByCardNumberForUpdate(second)
                    .orElseThrow(() -> new CardNotFoundException("Card not found: " + second));

            CardEntity fromCard = fromCardNumber.equals(first) ? firstCard : secondCard;
            CardEntity toCard = toCardNumber.equals(first) ? firstCard : secondCard;

            // 3) проверки статусов
            if (fromCard.getStatus() != CardStatus.ACTIVE) {
                throw new CardInactiveException("Source card is not active");
            }
            if (toCard.getStatus() != CardStatus.ACTIVE) {
                throw new CardInactiveException("Destination card is not active");
            }

            // 4) HOLD (reserve) + проверка доступного баланса
            BigDecimal reserved = nz(fromCard.getReservedBalance());
            BigDecimal available = fromCard.getBalance().subtract(reserved);

            if (available.compareTo(amount) < 0) {
                throw new InsufficientFundsException("Insufficient funds (available=" + available + ")");
            }

            fromCard.setReservedBalance(reserved.add(amount));
            fromCard.setUpdatedAt(LocalDateTime.now());
            cardRepository.save(fromCard);

            // 5) COMMIT: списание и начисление
            fromCard.setBalance(fromCard.getBalance().subtract(amount));
            fromCard.setReservedBalance(fromCard.getReservedBalance().subtract(amount));
            toCard.setBalance(toCard.getBalance().add(amount));

            fromCard.setUpdatedAt(LocalDateTime.now());
            toCard.setUpdatedAt(LocalDateTime.now());

            cardRepository.save(fromCard);
            cardRepository.save(toCard);

            // 6) SUCCESS (REQUIRES_NEW)
            transactionStatusService.markSuccess(txId);

            return TransferResponse.builder()
                    .transactionId(txId)
                    .fromCard(fromCardNumber)
                    .toCard(toCardNumber)
                    .amount(amount)
                    .status("SUCCESS")
                    .createdAt(LocalDateTime.now()) // или читай createdAt из tx если хочешь
                    .build();

        } catch (RuntimeException ex) {
            // ✅ FAILED (REQUIRES_NEW) — теперь точно найдёт запись, т.к. PENDING уже закоммичен
            transactionStatusService.markFailed(txId, ex.getMessage());
            throw ex;
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}