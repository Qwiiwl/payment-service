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
import uzumtech.paymentservice.service.tx.PhoneTopUpTxService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhoneTopUpServiceImplTest {

    @Mock CardRepository cardRepository;
    @Mock KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @Mock PhoneTopUpTxService phoneTopUpTxService;

    @InjectMocks PhoneTopUpServiceImpl phoneTopUpService;

    @Captor ArgumentCaptor<CardEntity> cardCaptor;
    @Captor ArgumentCaptor<String> phoneCaptor;
    @Captor ArgumentCaptor<BigDecimal> amountCaptor;

    @Test
    void topUp_whenCardNotFound_throwsCardNotFound() {
        PhoneTopUpRequest request = new PhoneTopUpRequest("8600", "+998901234567", new BigDecimal("10.00"));
        when(cardRepository.findByCardNumber("8600")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> phoneTopUpService.topUp(request))
                .isInstanceOf(CardNotFoundException.class)
                .hasMessageContaining("Card not found");

        verifyNoInteractions(phoneTopUpTxService);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void topUp_whenCardInactive_throwsCardInactive() {
        CardEntity card = CardEntity.builder()
                .id(1L)
                .cardNumber("8600")
                .balance(new BigDecimal("100.00"))
                .status(CardStatus.BLOCKED)
                .reservedBalance(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        PhoneTopUpRequest request = new PhoneTopUpRequest("8600", "+998901234567", new BigDecimal("10.00"));
        when(cardRepository.findByCardNumber("8600")).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> phoneTopUpService.topUp(request))
                .isInstanceOf(CardInactiveException.class)
                .hasMessageContaining("Card is not active");

        verifyNoInteractions(phoneTopUpTxService);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void topUp_whenInsufficientFunds_throwsInsufficientFunds() {
        CardEntity card = CardEntity.builder()
                .id(1L)
                .cardNumber("8600")
                .balance(new BigDecimal("5.00"))
                .status(CardStatus.ACTIVE)
                .reservedBalance(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        PhoneTopUpRequest request = new PhoneTopUpRequest("8600", "+998901234567", new BigDecimal("10.00"));
        when(cardRepository.findByCardNumber("8600")).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> phoneTopUpService.topUp(request))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds on card");

        // сервис не должен трогать тхсервис, иначе падаем
        verifyNoInteractions(phoneTopUpTxService);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void topUp_happyPath_callsTxService_andReturnsResponseFromTransaction() {
        CardEntity card = CardEntity.builder()
                .id(1L)
                .cardNumber("8600")
                .balance(new BigDecimal("100.00"))
                .status(CardStatus.ACTIVE)
                .reservedBalance(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        PhoneTopUpRequest request = new PhoneTopUpRequest("8600", "+998901234567", new BigDecimal("10.00"));
        when(cardRepository.findByCardNumber("8600")).thenReturn(Optional.of(card));

        UUID txId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        TransactionEntity tx = new TransactionEntity();
        tx.setTransactionId(txId);
        tx.setType(TransactionType.PHONE_TOPUP);
        tx.setSourceIdentifier("8600");
        tx.setDestinationIdentifier("+998901234567");
        tx.setAmount(new BigDecimal("10.00"));
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setCreatedAt(createdAt);
        tx.setUpdatedAt(createdAt);

        when(phoneTopUpTxService.applyTopUp(any(CardEntity.class), anyString(), any(BigDecimal.class)))
                .thenReturn(tx);

        PhoneTopUpResponse response = phoneTopUpService.topUp(request);

        // проверим аргументы вызова
        verify(phoneTopUpTxService).applyTopUp(cardCaptor.capture(), phoneCaptor.capture(), amountCaptor.capture());
        assertThat(cardCaptor.getValue()).isSameAs(card);
        assertThat(phoneCaptor.getValue()).isEqualTo("+998901234567");
        assertThat(amountCaptor.getValue()).isEqualByComparingTo("10.00");

        // респонс
        assertThat(response).isNotNull();
        assertThat(response.transactionId()).isEqualTo(txId);
        assertThat(response.fromCard()).isEqualTo("8600");
        assertThat(response.phoneNumber()).isEqualTo("+998901234567");
        assertThat(response.amount()).isEqualByComparingTo("10.00");
        assertThat(response.status()).isEqualTo(TransactionStatus.SUCCESS.name());
        assertThat(response.createdAt()).isEqualTo(createdAt);

        verifyNoInteractions(kafkaTemplate);
    }
}