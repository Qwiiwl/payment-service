package uzumtech.paymentservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
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
import uzumtech.paymentservice.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinePaymentServiceImplTest {

    @Mock
    CardRepository cardRepository;

    @Mock
    FineRepository fineRepository;

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    KafkaTemplate<String, TransactionEvent> kafkaTemplate; // в реализации не используется

    @InjectMocks
    FinePaymentServiceImpl finePaymentService;

    @Captor
    ArgumentCaptor<CardEntity> cardCaptor;

    @Captor
    ArgumentCaptor<FineEntity> fineCaptor;

    @Captor
    ArgumentCaptor<TransactionEntity> txCaptor;

    @BeforeEach
    void setup() {
        lenient().when(cardRepository.save(any(CardEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(fineRepository.save(any(FineEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void payFine_whenCardNotFound_throwsCardNotFound() {
        // given
        FinePaymentRequest request = new FinePaymentRequest("8600", 1L, new BigDecimal("10.00"));
        when(cardRepository.findByCardNumber("8600")).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> finePaymentService.payFine(request))
                .isInstanceOf(CardNotFoundException.class)
                .hasMessageContaining("Card not found");

        verifyNoInteractions(fineRepository, transactionRepository);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void payFine_whenCardInactive_throwsCardInactive() {
        // given
        CardEntity card = CardEntity.builder()
                .id(1L)
                .cardNumber("8600")
                .balance(new BigDecimal("100.00"))
                .status(CardStatus.BLOCKED)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .reservedBalance(BigDecimal.ZERO)
                .build();

        FinePaymentRequest request = new FinePaymentRequest("8600", 1L, new BigDecimal("10.00"));
        when(cardRepository.findByCardNumber("8600")).thenReturn(Optional.of(card));

        // when + then
        assertThatThrownBy(() -> finePaymentService.payFine(request))
                .isInstanceOf(CardInactiveException.class)
                .hasMessageContaining("Card is not active");

        verifyNoInteractions(fineRepository, transactionRepository);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void payFine_whenFineNotFound_throwsFineNotFound() {
        // given
        CardEntity card = CardEntity.builder()
                .id(1L)
                .cardNumber("8600")
                .balance(new BigDecimal("100.00"))
                .status(CardStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .reservedBalance(BigDecimal.ZERO)
                .build();

        FinePaymentRequest request = new FinePaymentRequest("8600", 999L, new BigDecimal("10.00"));

        when(cardRepository.findByCardNumber("8600")).thenReturn(Optional.of(card));
        when(fineRepository.findById(999L)).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> finePaymentService.payFine(request))
                .isInstanceOf(FineNotFoundException.class)
                .hasMessageContaining("Fine not found");

        verify(transactionRepository, never()).save(any());
        verify(cardRepository, never()).save(any());
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void payFine_whenFineAlreadyPaid_throwsFineAlreadyPaid() {
        // given
        CardEntity card = CardEntity.builder()
                .id(1L)
                .cardNumber("8600")
                .balance(new BigDecimal("100.00"))
                .status(CardStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .reservedBalance(BigDecimal.ZERO)
                .build();

        FineEntity fine = FineEntity.builder()
                .id(1L)
                .fineNumber("FINE-123")
                .amount(new BigDecimal("10.00"))
                .paid(true)
                .createdAt(LocalDateTime.now().minusDays(10))
                .paidAt(LocalDateTime.now().minusDays(1))
                .build();

        FinePaymentRequest request = new FinePaymentRequest("8600", 1L, new BigDecimal("10.00"));

        when(cardRepository.findByCardNumber("8600")).thenReturn(Optional.of(card));
        when(fineRepository.findById(1L)).thenReturn(Optional.of(fine));

        // when + then
        assertThatThrownBy(() -> finePaymentService.payFine(request))
                .isInstanceOf(FineAlreadyPaidException.class)
                .hasMessageContaining("Fine already paid");

        verify(transactionRepository, never()).save(any());
        verify(cardRepository, never()).save(any());
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void payFine_whenAmountNotEqualFineAmount_throwsIllegalArgument() {
        // given
        CardEntity card = CardEntity.builder()
                .id(1L)
                .cardNumber("8600")
                .balance(new BigDecimal("100.00"))
                .status(CardStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .reservedBalance(BigDecimal.ZERO)
                .build();

        FineEntity fine = FineEntity.builder()
                .id(1L)
                .fineNumber("FINE-123")
                .amount(new BigDecimal("10.00"))
                .paid(false)
                .createdAt(LocalDateTime.now().minusDays(10))
                .build();

        FinePaymentRequest request = new FinePaymentRequest("8600", 1L, new BigDecimal("9.99"));

        when(cardRepository.findByCardNumber("8600")).thenReturn(Optional.of(card));
        when(fineRepository.findById(1L)).thenReturn(Optional.of(fine));

        // when + then
        assertThatThrownBy(() -> finePaymentService.payFine(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must equal fine amount");

        verify(transactionRepository, never()).save(any());
        verify(cardRepository, never()).save(any());
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void payFine_whenInsufficientFunds_throwsInsufficientFunds() {
        // given
        CardEntity card = CardEntity.builder()
                .id(1L)
                .cardNumber("8600")
                .balance(new BigDecimal("5.00"))
                .status(CardStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .reservedBalance(BigDecimal.ZERO)
                .build();

        FineEntity fine = FineEntity.builder()
                .id(1L)
                .fineNumber("FINE-123")
                .amount(new BigDecimal("10.00"))
                .paid(false)
                .createdAt(LocalDateTime.now().minusDays(10))
                .build();

        FinePaymentRequest request = new FinePaymentRequest("8600", 1L, new BigDecimal("10.00"));

        when(cardRepository.findByCardNumber("8600")).thenReturn(Optional.of(card));
        when(fineRepository.findById(1L)).thenReturn(Optional.of(fine));

        // when + then
        assertThatThrownBy(() -> finePaymentService.payFine(request))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds on card");

        verify(transactionRepository, never()).save(any());
        verify(cardRepository, never()).save(any());
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void payFine_happyPath_updatesCardAndFine_savesTransaction_returnsResponse() {
        // given
        LocalDateTime oldUpdated = LocalDateTime.now().minusDays(1);

        CardEntity card = CardEntity.builder()
                .id(1L)
                .cardNumber("8600")
                .balance(new BigDecimal("100.00"))
                .status(CardStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(oldUpdated)
                .reservedBalance(BigDecimal.ZERO)
                .build();

        FineEntity fine = FineEntity.builder()
                .id(7L)
                .fineNumber("FINE-777")
                .amount(new BigDecimal("10.00"))
                .paid(false)
                .createdAt(LocalDateTime.now().minusDays(10))
                .paidAt(null)
                .build();

        FinePaymentRequest request = new FinePaymentRequest("8600", 7L, new BigDecimal("10.00"));

        when(cardRepository.findByCardNumber("8600")).thenReturn(Optional.of(card));
        when(fineRepository.findById(7L)).thenReturn(Optional.of(fine));

        // when
        FinePaymentResponse response = finePaymentService.payFine(request);

        // then: карта списалась
        verify(cardRepository).save(cardCaptor.capture());
        CardEntity savedCard = cardCaptor.getValue();
        assertThat(savedCard.getBalance()).isEqualByComparingTo("90.00");
        assertThat(savedCard.getUpdatedAt()).isNotNull();
        assertThat(savedCard.getUpdatedAt()).isAfterOrEqualTo(oldUpdated);

        // then: штраф стал paid=true
        verify(fineRepository).save(fineCaptor.capture());
        FineEntity savedFine = fineCaptor.getValue();
        assertThat(savedFine.getPaid()).isTrue();
        assertThat(savedFine.getPaidAt()).isNotNull();

        // then: транзакция сохранилась корректно
        verify(transactionRepository).save(txCaptor.capture());
        TransactionEntity tx = txCaptor.getValue();

        assertThat(tx.getTransactionId()).isNotNull();
        assertThat(tx.getType()).isEqualTo(TransactionType.FINE_PAYMENT);
        assertThat(tx.getSourceIdentifier()).isEqualTo("8600");
        assertThat(tx.getDestinationIdentifier()).isEqualTo("FINE#7");
        assertThat(tx.getAmount()).isEqualByComparingTo("10.00");
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(tx.getCreatedAt()).isNotNull();

        // then: response соответствует транзакции
        assertThat(response.transactionId()).isEqualTo(tx.getTransactionId());
        assertThat(response.fromCard()).isEqualTo("8600");
        assertThat(response.fineNumber()).isEqualTo("FINE-777");
        assertThat(response.amount()).isEqualByComparingTo("10.00");
        assertThat(response.status()).isEqualTo(TransactionStatus.SUCCESS.name());
        assertThat(response.createdAt()).isEqualTo(tx.getCreatedAt());

        // Kafka в текущей реализации не используется
        verifyNoInteractions(kafkaTemplate);
    }
}