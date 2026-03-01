package uzumtech.paymentservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import uzumtech.paymentservice.constant.enums.CardStatus;
import uzumtech.paymentservice.constant.enums.TransactionStatus;
import uzumtech.paymentservice.constant.enums.TransactionType;
import uzumtech.paymentservice.dto.TransactionEvent;
import uzumtech.paymentservice.dto.request.FinePaymentRequest;
import uzumtech.paymentservice.dto.response.FinePaymentResponse;
import uzumtech.paymentservice.entity.CardEntity;
import uzumtech.paymentservice.entity.FineEntity;
import uzumtech.paymentservice.entity.TransactionEntity;
import uzumtech.paymentservice.exception.*;
import uzumtech.paymentservice.repository.CardRepository;
import uzumtech.paymentservice.repository.FineRepository;
import uzumtech.paymentservice.service.tx.FinePaymentTxService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinePaymentServiceImplTest {

    @Mock CardRepository cardRepository;
    @Mock FineRepository fineRepository;

    @Mock FinePaymentTxService finePaymentTxService;

    @InjectMocks FinePaymentServiceImpl finePaymentService;

    @Captor ArgumentCaptor<CardEntity> cardCaptor;
    @Captor ArgumentCaptor<FineEntity> fineCaptor;
    @Captor ArgumentCaptor<BigDecimal> amountCaptor;

    @BeforeEach
    void setup() {
        // здесь можно ничего не делать
    }

    @Test
    void payFine_whenCardNotFound_throwsCardNotFound() {
        when(cardRepository.findByCardNumber("8600")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> finePaymentService.payFine(
                new FinePaymentRequest("8600", 1L, new BigDecimal("10.00"))
        ))
                .isInstanceOf(CardNotFoundException.class)
                .hasMessageContaining("Card not found");

        verifyNoInteractions(fineRepository, finePaymentTxService);
    }

    @Test
    void payFine_whenCardInactive_throwsCardInactive() {
        CardEntity card = CardEntity.builder()
                .id(1L)
                .cardNumber("8600")
                .balance(new BigDecimal("100.00"))
                .status(CardStatus.BLOCKED)
                .reservedBalance(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        when(cardRepository.findByCardNumber("8600")).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> finePaymentService.payFine(
                new FinePaymentRequest("8600", 1L, new BigDecimal("10.00"))
        ))
                .isInstanceOf(CardInactiveException.class)
                .hasMessageContaining("Card is not active");

        verifyNoInteractions(fineRepository, finePaymentTxService);
    }

    @Test
    void payFine_whenFineNotFound_throwsFineNotFound() {
        CardEntity card = CardEntity.builder()
                .id(1L)
                .cardNumber("8600")
                .balance(new BigDecimal("100.00"))
                .status(CardStatus.ACTIVE)
                .reservedBalance(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        when(cardRepository.findByCardNumber("8600")).thenReturn(Optional.of(card));
        when(fineRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> finePaymentService.payFine(
                new FinePaymentRequest("8600", 999L, new BigDecimal("10.00"))
        ))
                .isInstanceOf(FineNotFoundException.class)
                .hasMessageContaining("Fine not found");

        verifyNoInteractions(finePaymentTxService);
    }

    @Test
    void payFine_whenFineAlreadyPaid_throwsFineAlreadyPaid() {
        CardEntity card = CardEntity.builder()
                .id(1L)
                .cardNumber("8600")
                .balance(new BigDecimal("100.00"))
                .status(CardStatus.ACTIVE)
                .reservedBalance(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        FineEntity fine = FineEntity.builder()
                .id(1L)
                .fineNumber("FINE-123")
                .amount(new BigDecimal("10.00"))
                .paid(true)
                .createdAt(LocalDateTime.now().minusDays(10))
                .paidAt(LocalDateTime.now().minusDays(1))
                .build();

        when(cardRepository.findByCardNumber("8600")).thenReturn(Optional.of(card));
        when(fineRepository.findById(1L)).thenReturn(Optional.of(fine));

        assertThatThrownBy(() -> finePaymentService.payFine(
                new FinePaymentRequest("8600", 1L, new BigDecimal("10.00"))
        ))
                .isInstanceOf(FineAlreadyPaidException.class)
                .hasMessageContaining("Fine already paid");

        verifyNoInteractions(finePaymentTxService);
    }

    @Test
    void payFine_whenAmountNullOrNotEqualFineAmount_throwsIllegalArgument() {
        CardEntity card = CardEntity.builder()
                .id(1L)
                .cardNumber("8600")
                .balance(new BigDecimal("100.00"))
                .status(CardStatus.ACTIVE)
                .reservedBalance(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        FineEntity fine = FineEntity.builder()
                .id(1L)
                .fineNumber("FINE-123")
                .amount(new BigDecimal("10.00"))
                .paid(false)
                .createdAt(LocalDateTime.now().minusDays(10))
                .build();

        when(cardRepository.findByCardNumber("8600")).thenReturn(Optional.of(card));
        when(fineRepository.findById(1L)).thenReturn(Optional.of(fine));

        assertThatThrownBy(() -> finePaymentService.payFine(
                new FinePaymentRequest("8600", 1L, null)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must equal fine amount");

        assertThatThrownBy(() -> finePaymentService.payFine(
                new FinePaymentRequest("8600", 1L, new BigDecimal("9.99"))
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must equal fine amount");

        verifyNoInteractions(finePaymentTxService);
    }

    @Test
    void payFine_whenInsufficientFunds_throwsInsufficientFunds() {
        CardEntity card = CardEntity.builder()
                .id(1L)
                .cardNumber("8600")
                .balance(new BigDecimal("5.00"))
                .status(CardStatus.ACTIVE)
                .reservedBalance(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        FineEntity fine = FineEntity.builder()
                .id(1L)
                .fineNumber("FINE-123")
                .amount(new BigDecimal("10.00"))
                .paid(false)
                .createdAt(LocalDateTime.now().minusDays(10))
                .build();

        when(cardRepository.findByCardNumber("8600")).thenReturn(Optional.of(card));
        when(fineRepository.findById(1L)).thenReturn(Optional.of(fine));

        assertThatThrownBy(() -> finePaymentService.payFine(
                new FinePaymentRequest("8600", 1L, new BigDecimal("10.00"))
        ))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds on card");

        verifyNoInteractions(finePaymentTxService);
    }

    @Test
    void payFine_happyPath_callsTxService_andReturnsResponseFromTransaction() {
        // given
        CardEntity card = CardEntity.builder()
                .id(1L)
                .cardNumber("8600")
                .balance(new BigDecimal("100.00"))
                .status(CardStatus.ACTIVE)
                .reservedBalance(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        FineEntity fine = FineEntity.builder()
                .id(7L)
                .fineNumber("FINE-777")
                .amount(new BigDecimal("10.00"))
                .paid(false)
                .createdAt(LocalDateTime.now().minusDays(10))
                .paidAt(null)
                .build();

        when(cardRepository.findByCardNumber("8600")).thenReturn(Optional.of(card));
        when(fineRepository.findById(7L)).thenReturn(Optional.of(fine));

        UUID txId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        TransactionEntity tx = new TransactionEntity();
        tx.setTransactionId(txId);
        tx.setType(TransactionType.FINE_PAYMENT);
        tx.setSourceIdentifier("8600");
        tx.setDestinationIdentifier("FINE#7");
        tx.setAmount(new BigDecimal("10.00"));
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setCreatedAt(createdAt);
        tx.setUpdatedAt(createdAt);

        when(finePaymentTxService.applyFinePayment(any(CardEntity.class), any(FineEntity.class), any(BigDecimal.class)))
                .thenReturn(tx);

        // when
        FinePaymentResponse response = finePaymentService.payFine(
                new FinePaymentRequest("8600", 7L, new BigDecimal("10.00"))
        );

        // then: вызывали TX-service с нужными аргументами
        verify(finePaymentTxService).applyFinePayment(cardCaptor.capture(), fineCaptor.capture(), amountCaptor.capture());
        assertThat(cardCaptor.getValue()).isSameAs(card);
        assertThat(fineCaptor.getValue()).isSameAs(fine);
        assertThat(amountCaptor.getValue()).isEqualByComparingTo("10.00");

        // and: response = из TransactionEntity
        assertThat(response).isNotNull();
        assertThat(response.transactionId()).isEqualTo(txId);
        assertThat(response.fromCard()).isEqualTo("8600");
        assertThat(response.fineNumber()).isEqualTo("FINE-777");
        assertThat(response.amount()).isEqualByComparingTo("10.00");
        assertThat(response.status()).isEqualTo(TransactionStatus.SUCCESS.name());
        assertThat(response.createdAt()).isEqualTo(createdAt);

    }
}