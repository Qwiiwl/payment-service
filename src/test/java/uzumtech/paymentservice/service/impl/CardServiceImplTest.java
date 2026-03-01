package uzumtech.paymentservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import uzumtech.paymentservice.constant.enums.CardStatus;
import uzumtech.paymentservice.constant.enums.OtpStatus;
import uzumtech.paymentservice.dto.request.CardAddRequest;
import uzumtech.paymentservice.dto.request.CardConfirmRequest;
import uzumtech.paymentservice.dto.response.CardAddResponse;
import uzumtech.paymentservice.dto.response.CardConfirmResponse;
import uzumtech.paymentservice.entity.CardEntity;
import uzumtech.paymentservice.entity.OtpEntity;
import uzumtech.paymentservice.exception.CardAlreadyExistsException;
import uzumtech.paymentservice.exception.InvalidOtpException;
import uzumtech.paymentservice.exception.OtpExpiredException;
import uzumtech.paymentservice.repository.CardRepository;
import uzumtech.paymentservice.repository.OtpRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    @Mock
    CardRepository cardRepository;

    @Mock
    OtpRepository otpRepository;

    @InjectMocks
    CardServiceImpl cardService;

    @Captor
    ArgumentCaptor<OtpEntity> otpCaptor;

    @Captor
    ArgumentCaptor<CardEntity> cardCaptor;

    @BeforeEach
    void setup() {
        // по умолчанию save возвращает то, что сохранили
        lenient().when(otpRepository.save(any(OtpEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(cardRepository.save(any(CardEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void initiateCardAdding_savesOtpAndReturnsOtpSent() {
        // given
        CardAddRequest request = new CardAddRequest(10L, "8600123412341234");

        // when
        CardAddResponse response = cardService.initiateCardAdding(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.message()).isEqualTo("OTP_SENT");

        verify(otpRepository).save(otpCaptor.capture());
        OtpEntity saved = otpCaptor.getValue();

        assertThat(saved.getUserId()).isEqualTo(10L);
        assertThat(saved.getCardNumber()).isEqualTo("8600123412341234");
        assertThat(saved.getStatus()).isEqualTo(OtpStatus.ACTIVE);

        assertThat(saved.getCode()).matches("\\d{6}");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getExpireAt()).isNotNull();
        assertThat(saved.getExpireAt()).isAfter(saved.getCreatedAt());
    }

    @Test
    void confirmCardAdding_whenOtpNotFound_throwsInvalidOtp() {
        // given
        CardConfirmRequest request = new CardConfirmRequest(10L, "8600123412341234", "123456");
        when(otpRepository.findTopByUserIdAndCardNumberAndStatusOrderByCreatedAtDesc(
                10L, "8600123412341234", OtpStatus.ACTIVE
        )).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> cardService.confirmCardAdding(request))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("OTP not found");

        verify(cardRepository, never()).save(any());
        verify(otpRepository, never()).save(any()); // кроме find ничего не сохраняем
    }

    @Test
    void confirmCardAdding_whenOtpExpired_marksExpiredAndThrows() {
        // given
        OtpEntity otp = OtpEntity.builder()
                .id(1L)
                .userId(10L)
                .cardNumber("8600123412341234")
                .code("123456")
                .status(OtpStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .expireAt(LocalDateTime.now().minusMinutes(1)) // уже истёк
                .build();

        when(otpRepository.findTopByUserIdAndCardNumberAndStatusOrderByCreatedAtDesc(
                10L, "8600123412341234", OtpStatus.ACTIVE
        )).thenReturn(Optional.of(otp));

        CardConfirmRequest request = new CardConfirmRequest(10L, "8600123412341234", "123456");

        // when + then
        assertThatThrownBy(() -> cardService.confirmCardAdding(request))
                .isInstanceOf(OtpExpiredException.class)
                .hasMessageContaining("OTP expired");

        // должен поменять статус на EXPIRED и сохранить
        verify(otpRepository, atLeastOnce()).save(otpCaptor.capture());
        assertThat(otpCaptor.getAllValues())
                .anySatisfy(saved -> assertThat(saved.getStatus()).isEqualTo(OtpStatus.EXPIRED));

        verify(cardRepository, never()).save(any());
    }

    @Test
    void confirmCardAdding_whenOtpCodeWrong_throwsInvalidOtp() {
        // given
        OtpEntity otp = OtpEntity.builder()
                .id(1L)
                .userId(10L)
                .cardNumber("8600123412341234")
                .code("111111")
                .status(OtpStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .expireAt(LocalDateTime.now().plusMinutes(1))
                .build();

        when(otpRepository.findTopByUserIdAndCardNumberAndStatusOrderByCreatedAtDesc(
                10L, "8600123412341234", OtpStatus.ACTIVE
        )).thenReturn(Optional.of(otp));

        CardConfirmRequest request = new CardConfirmRequest(10L, "8600123412341234", "222222");

        // when + then
        assertThatThrownBy(() -> cardService.confirmCardAdding(request))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("Invalid OTP code");

        verify(cardRepository, never()).save(any());
        // статус USED/EXPIRED выставляться не должен
        verify(otpRepository, never()).save(any(OtpEntity.class));
    }

    @Test
    void confirmCardAdding_whenCardAlreadyExists_throwsCardAlreadyExists() {
        // given
        OtpEntity otp = OtpEntity.builder()
                .id(1L)
                .userId(10L)
                .cardNumber("8600123412341234")
                .code("123456")
                .status(OtpStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .expireAt(LocalDateTime.now().plusMinutes(1))
                .build();

        when(otpRepository.findTopByUserIdAndCardNumberAndStatusOrderByCreatedAtDesc(
                10L, "8600123412341234", OtpStatus.ACTIVE
        )).thenReturn(Optional.of(otp));

        when(cardRepository.findByCardNumber("8600123412341234"))
                .thenReturn(Optional.of(CardEntity.builder()
                        .id(99L)
                        .cardNumber("8600123412341234")
                        .balance(BigDecimal.ZERO)
                        .status(CardStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));

        CardConfirmRequest request = new CardConfirmRequest(10L, "8600123412341234", "123456");

        // when + then
        assertThatThrownBy(() -> cardService.confirmCardAdding(request))
                .isInstanceOf(CardAlreadyExistsException.class)
                .hasMessageContaining("Card already exists");

        verify(cardRepository, never()).save(any());
        verify(otpRepository, never()).save(any());
    }

    @Test
    void confirmCardAdding_happyPath_createsCard_andMarksOtpUsed() {
        // given
        OtpEntity otp = OtpEntity.builder()
                .id(1L)
                .userId(10L)
                .cardNumber("8600123412341234")
                .code("123456")
                .status(OtpStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .expireAt(LocalDateTime.now().plusMinutes(1))
                .build();

        when(otpRepository.findTopByUserIdAndCardNumberAndStatusOrderByCreatedAtDesc(
                10L, "8600123412341234", OtpStatus.ACTIVE
        )).thenReturn(Optional.of(otp));

        when(cardRepository.findByCardNumber("8600123412341234")).thenReturn(Optional.empty());

        // when
        CardConfirmResponse response = cardService.confirmCardAdding(
                new CardConfirmRequest(10L, "8600123412341234", "123456")
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.cardNumber()).isEqualTo("8600123412341234");
        assertThat(response.status()).isEqualTo(CardStatus.ACTIVE.name());
        assertThat(response.createdAt()).isNotNull();

        verify(cardRepository).save(cardCaptor.capture());
        CardEntity savedCard = cardCaptor.getValue();
        assertThat(savedCard.getCardNumber()).isEqualTo("8600123412341234");
        assertThat(savedCard.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(savedCard.getStatus()).isEqualTo(CardStatus.ACTIVE);

        // OTP должен стать USED и сохраниться
        verify(otpRepository).save(otpCaptor.capture());
        assertThat(otpCaptor.getValue().getStatus()).isEqualTo(OtpStatus.USED);
    }
}