package uzumtech.paymentservice.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import uzumtech.paymentservice.constant.enums.CardStatus;
import uzumtech.paymentservice.constant.enums.TransactionStatus;
import uzumtech.paymentservice.constant.enums.TransactionType;
import uzumtech.paymentservice.dto.TransactionEvent;
import uzumtech.paymentservice.dto.request.PhoneTopUpRequest;
import uzumtech.paymentservice.dto.response.PhoneTopUpResponse;
import uzumtech.paymentservice.entity.CardEntity;
import uzumtech.paymentservice.entity.TransactionEntity;
import uzumtech.paymentservice.exception.CardInactiveException;
import uzumtech.paymentservice.exception.CardNotFoundException;
import uzumtech.paymentservice.exception.InsufficientFundsException;
import uzumtech.paymentservice.repository.CardRepository;
import uzumtech.paymentservice.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhoneTopUpServiceImplTest {

    @Mock
    CardRepository cardRepository;

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    KafkaTemplate<String, TransactionEvent> kafkaTemplate; // в текущей версии сервиса не используется

    @InjectMocks
    PhoneTopUpServiceImpl phoneTopUpService;

    @Captor
    ArgumentCaptor<CardEntity> cardCaptor;

    @Captor
    ArgumentCaptor<TransactionEntity> txCaptor;

    @Test
    void topUp_whenCardNotFound_throwsCardNotFound() {
        // given
        PhoneTopUpRequest request = new PhoneTopUpRequest("8600", "+998901234567", new BigDecimal("10.00"));
        when(cardRepository.findByCardNumber("8600")).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> phoneTopUpService.topUp(request))
                .isInstanceOf(CardNotFoundException.class)
                .hasMessageContaining("Card not found");

        verify(transactionRepository, never()).save(any());
        verify(cardRepository, never()).save(any());
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void topUp_whenCardInactive_throwsCardInactive() {
        // given
        CardEntity card = CardEntity.builder()
                .id(1L)
                .cardNumber("8600")
                .balance(new BigDecimal("100.00"))
                .status(CardStatus.BLOCKED) // не ACTIVE
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .reservedBalance(BigDecimal.ZERO)
                .build();

        PhoneTopUpRequest request = new PhoneTopUpRequest("8600", "+998901234567", new BigDecimal("10.00"));
        when(cardRepository.findByCardNumber("8600")).thenReturn(Optional.of(card));

        // when + then
        assertThatThrownBy(() -> phoneTopUpService.topUp(request))
                .isInstanceOf(CardInactiveException.class)
                .hasMessageContaining("Card is not active");

        verify(cardRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void topUp_whenInsufficientFunds_throwsInsufficientFunds() {
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

        PhoneTopUpRequest request = new PhoneTopUpRequest("8600", "+998901234567", new BigDecimal("10.00"));
        when(cardRepository.findByCardNumber("8600")).thenReturn(Optional.of(card));

        // when + then
        assertThatThrownBy(() -> phoneTopUpService.topUp(request))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds on card");

        // состояние не меняем и ничего не сохраняем
        assertThat(card.getBalance()).isEqualByComparingTo("5.00");
        verify(cardRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void topUp_happyPath_decreasesBalance_savesCardAndTransaction_returnsResponse() {
        // given
        LocalDateTime oldUpdatedAt = LocalDateTime.now().minusDays(1);

        CardEntity card = CardEntity.builder()
                .id(1L)
                .cardNumber("8600")
                .balance(new BigDecimal("100.00"))
                .status(CardStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(oldUpdatedAt)
                .reservedBalance(BigDecimal.ZERO)
                .build();

        PhoneTopUpRequest request = new PhoneTopUpRequest("8600", "+998901234567", new BigDecimal("10.00"));

        when(cardRepository.findByCardNumber("8600")).thenReturn(Optional.of(card));
        when(cardRepository.save(any(CardEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        PhoneTopUpResponse response = phoneTopUpService.topUp(request);

        // карта сохранилась с новым балансом
        verify(cardRepository).save(cardCaptor.capture());
        CardEntity savedCard = cardCaptor.getValue();

        assertThat(savedCard.getCardNumber()).isEqualTo("8600");
        assertThat(savedCard.getBalance()).isEqualByComparingTo("90.00");
        assertThat(savedCard.getUpdatedAt()).isNotNull();
        assertThat(savedCard.getUpdatedAt()).isAfterOrEqualTo(oldUpdatedAt);

        // транзакция сохранилась с нужными полями
        verify(transactionRepository).save(txCaptor.capture());
        TransactionEntity savedTx = txCaptor.getValue();

        assertThat(savedTx.getTransactionId()).isNotNull();
        assertThat(savedTx.getType()).isEqualTo(TransactionType.PHONE_TOPUP);
        assertThat(savedTx.getSourceIdentifier()).isEqualTo("8600");
        assertThat(savedTx.getDestinationIdentifier()).isEqualTo("+998901234567");
        assertThat(savedTx.getAmount()).isEqualByComparingTo("10.00");
        assertThat(savedTx.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(savedTx.getCreatedAt()).isNotNull();

        // then: response совпадает с транзакцией
        assertThat(response).isNotNull();
        assertThat(response.transactionId()).isEqualTo(savedTx.getTransactionId());
        assertThat(response.fromCard()).isEqualTo("8600");
        assertThat(response.phoneNumber()).isEqualTo("+998901234567");
        assertThat(response.amount()).isEqualByComparingTo("10.00");
        assertThat(response.status()).isEqualTo(TransactionStatus.SUCCESS.name());
        assertThat(response.createdAt()).isEqualTo(savedTx.getCreatedAt());

        // Kafka сейчас не используется — проверим, что туда не лезем
        verifyNoInteractions(kafkaTemplate);
    }
}