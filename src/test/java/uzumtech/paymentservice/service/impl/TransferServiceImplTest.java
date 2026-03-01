package uzumtech.paymentservice.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import uzumtech.paymentservice.constant.enums.CardStatus;
import uzumtech.paymentservice.constant.enums.TransactionType;
import uzumtech.paymentservice.dto.response.TransferResponse;
import uzumtech.paymentservice.entity.CardEntity;
import uzumtech.paymentservice.exception.InsufficientFundsException;
import uzumtech.paymentservice.repository.CardRepository;
import uzumtech.paymentservice.service.TransactionStatusService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock
    CardRepository cardRepository;

    @Mock
    TransactionStatusService transactionStatusService;

    @InjectMocks
    TransferServiceImpl transferService;

    @Captor
    ArgumentCaptor<CardEntity> cardCaptor;

    @Test
    void transfer_happyPath_marksSuccessAndMovesMoney() {
        // given
        String from = "8600000000000001";
        String to = "8600000000000002";
        BigDecimal amount = new BigDecimal("50");

        UUID txId = UUID.randomUUID();
        when(transactionStatusService.createPending(eq(TransactionType.TRANSFER), eq(from), eq(to), eq(amount)))
                .thenReturn(txId);

        CardEntity fromCard = CardEntity.builder()
                .id(1L)
                .cardNumber(from)
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("100"))
                .reservedBalance(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        CardEntity toCard = CardEntity.builder()
                .id(2L)
                .cardNumber(to)
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("10"))
                .reservedBalance(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // сервис сам решает порядок блокировки по compareTo — поэтому мокнем оба номера
        when(cardRepository.findByCardNumberForUpdate(anyString()))
                .thenAnswer(inv -> {
                    String cn = inv.getArgument(0);
                    if (cn.equals(from)) return Optional.of(fromCard);
                    if (cn.equals(to)) return Optional.of(toCard);
                    return Optional.empty();
                });

        when(cardRepository.save(any(CardEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        TransferResponse response = transferService.transfer(from, to, amount);

        // then
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.transactionId()).isEqualTo(txId);
        assertThat(response.amount()).isEqualByComparingTo(amount);

        verify(transactionStatusService).markSuccess(txId);
        verify(transactionStatusService, never()).markFailed(any(), any());

        // fromCard: 100 - 50 = 50
        assertThat(fromCard.getBalance()).isEqualByComparingTo(new BigDecimal("50"));
        // reservedBalance должен вернуться в 0 после коммита
        assertThat(fromCard.getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        // toCard: 10 + 50 = 60
        assertThat(toCard.getBalance()).isEqualByComparingTo(new BigDecimal("60"));

        // HOLD (fromCard), потом fromCard и toCard
        verify(cardRepository, atLeast(3)).save(any(CardEntity.class));
    }

    @Test
    void transfer_whenInsufficientFunds_marksFailedAndThrows() {
        // given
        String from = "8600000000000001";
        String to = "8600000000000002";
        BigDecimal amount = new BigDecimal("50");

        UUID txId = UUID.randomUUID();
        when(transactionStatusService.createPending(eq(TransactionType.TRANSFER), eq(from), eq(to), eq(amount)))
                .thenReturn(txId);

        CardEntity fromCard = CardEntity.builder()
                .id(1L)
                .cardNumber(from)
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("10"))
                .reservedBalance(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        CardEntity toCard = CardEntity.builder()
                .id(2L)
                .cardNumber(to)
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("10"))
                .reservedBalance(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cardRepository.findByCardNumberForUpdate(anyString()))
                .thenAnswer(inv -> {
                    String cn = inv.getArgument(0);
                    if (cn.equals(from)) return Optional.of(fromCard);
                    if (cn.equals(to)) return Optional.of(toCard);
                    return Optional.empty();
                });

        // when + then
        assertThatThrownBy(() -> transferService.transfer(from, to, amount))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");

        verify(transactionStatusService).markFailed(eq(txId), contains("Insufficient funds"));
        verify(transactionStatusService, never()).markSuccess(any());

        // денег не хватило никакие save по картам не должны происходить
        verify(cardRepository, never()).save(any());
    }
}