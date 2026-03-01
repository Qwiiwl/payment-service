package uzumtech.paymentservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uzumtech.paymentservice.constant.enums.CardStatus;
import uzumtech.paymentservice.entity.CardEntity;
import uzumtech.paymentservice.exception.CardInactiveException;
import uzumtech.paymentservice.exception.CardNotFoundException;
import uzumtech.paymentservice.exception.InsufficientFundsException;
import uzumtech.paymentservice.repository.CardRepository;
import uzumtech.paymentservice.service.BillingService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransferBillingServiceImpl implements BillingService {

    private final CardRepository cardRepository;


     //Транзакция только вокруг изменения балансов/резерва и сохранений
    @Override
    @Transactional
    public void transferBetweenCards(String fromCardNumber, String toCardNumber, BigDecimal amount) {
        // Блокируем обе карты в одном и том же порядке, чтобы избежать дедлоков
        String first = fromCardNumber.compareTo(toCardNumber) <= 0 ? fromCardNumber : toCardNumber;
        String second = first.equals(fromCardNumber) ? toCardNumber : fromCardNumber;

        CardEntity firstCard = cardRepository.findByCardNumberForUpdate(first)
                .orElseThrow(() -> new CardNotFoundException("Card not found: " + first));
        CardEntity secondCard = cardRepository.findByCardNumberForUpdate(second)
                .orElseThrow(() -> new CardNotFoundException("Card not found: " + second));

        CardEntity fromCard = fromCardNumber.equals(first) ? firstCard : secondCard;
        CardEntity toCard = toCardNumber.equals(first) ? firstCard : secondCard;

        validateCards(fromCard, toCard);
        holdFunds(fromCard, amount);
        commitTransfer(fromCard, toCard, amount);

        cardRepository.save(fromCard);
        cardRepository.save(toCard);
    }

    private void validateCards(CardEntity fromCard, CardEntity toCard) {
        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardInactiveException("Source card is not active");
        }
        if (toCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardInactiveException("Destination card is not active");
        }
    }

    private void holdFunds(CardEntity fromCard, BigDecimal amount) {
        BigDecimal reserved = nz(fromCard.getReservedBalance());
        BigDecimal available = fromCard.getBalance().subtract(reserved);

        if (available.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds (available=" + available + ")");
        }

        fromCard.setReservedBalance(reserved.add(amount));
        fromCard.setUpdatedAt(LocalDateTime.now());
    }

    private void commitTransfer(CardEntity fromCard, CardEntity toCard, BigDecimal amount) {
        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        fromCard.setReservedBalance(fromCard.getReservedBalance().subtract(amount));
        toCard.setBalance(toCard.getBalance().add(amount));

        fromCard.setUpdatedAt(LocalDateTime.now());
        toCard.setUpdatedAt(LocalDateTime.now());
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
