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
import uzumtech.paymentservice.entity.UserEntity;
import uzumtech.paymentservice.exception.CardAlreadyExistsException;
import uzumtech.paymentservice.exception.InvalidOtpException;
import uzumtech.paymentservice.exception.OtpExpiredException;
import uzumtech.paymentservice.mapper.CardMapper;
import uzumtech.paymentservice.repository.CardRepository;
import uzumtech.paymentservice.repository.OtpRepository;
import uzumtech.paymentservice.repository.UserRepository;
import uzumtech.paymentservice.service.OtpService;
import uzumtech.paymentservice.service.tx.CardTxService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    @Mock CardRepository cardRepository;
    @Mock OtpRepository otpRepository;

    @Mock UserRepository userRepository;
    @Mock OtpService otpService;

    @Mock CardTxService cardTxService;
    @Mock CardMapper cardMapper;

    @InjectMocks CardServiceImpl cardService;

    @Captor ArgumentCaptor<Long> userIdCaptor;
    @Captor ArgumentCaptor<String> cardNumberCaptor;
    @Captor ArgumentCaptor<String> emailCaptor;
    @Captor ArgumentCaptor<OtpEntity> otpCaptor;
    @Captor ArgumentCaptor<CardEntity> cardCaptor;

    @BeforeEach
    void setup() {
        lenient().when(cardTxService.createCardAndMarkOtpUsed(any(CardEntity.class), any(OtpEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }


    @Test
    void initiateCardAdding_whenCardAlreadyExists_throwsCardAlreadyExists() {
        when(cardRepository.findByCardNumber("8600123412341234"))
                .thenReturn(Optional.of(new CardEntity()));

        assertThatThrownBy(() -> cardService.initiateCardAdding(
                new CardAddRequest(10L, "8600123412341234")
        ))
                .isInstanceOf(CardAlreadyExistsException.class)
                .hasMessageContaining("Card already exists");

        verifyNoInteractions(userRepository, otpService);
    }

    @Test
    void initiateCardAdding_whenUserNotFound_throwsIllegalArgument() {
        when(cardRepository.findByCardNumber("8600123412341234"))
                .thenReturn(Optional.empty());
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.initiateCardAdding(
                new CardAddRequest(10L, "8600123412341234")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        verifyNoInteractions(otpService);
    }

    @Test
    void initiateCardAdding_happyPath_callsOtpService_returnsMessage() {
        when(cardRepository.findByCardNumber("8600123412341234"))
                .thenReturn(Optional.empty());

        UserEntity user = new UserEntity();
        user.setId(10L);
        user.setEmail("test@mail.com");
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        CardAddResponse response = cardService.initiateCardAdding(
                new CardAddRequest(10L, "8600123412341234")
        );

        assertThat(response).isNotNull();
        assertThat(response.message()).isEqualTo("OTP sent to email for card adding");

        verify(otpService).createAndSendOtp(
                userIdCaptor.capture(),
                cardNumberCaptor.capture(),
                isNull(),
                emailCaptor.capture()
        );

        assertThat(userIdCaptor.getValue()).isEqualTo(10L);
        assertThat(cardNumberCaptor.getValue()).isEqualTo("8600123412341234");
        assertThat(emailCaptor.getValue()).isEqualTo("test@mail.com");

        verify(otpRepository, never()).save(any());
    }


    @Test
    void confirmCardAdding_whenOtpNotFound_throwsInvalidOtp() {
        when(otpRepository.findTopByUserIdAndCardNumberAndStatusOrderByCreatedAtDesc(
                10L, "8600123412341234", OtpStatus.ACTIVE
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.confirmCardAdding(
                new CardConfirmRequest(10L, "8600123412341234", "123456")
        ))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("OTP not found");

        verifyNoInteractions(cardTxService, cardMapper);
    }

    @Test
    void confirmCardAdding_whenOtpExpired_marksExpired_viaTxService_andThrows() {
        OtpEntity otp = new OtpEntity();
        otp.setId(1L);
        otp.setUserId(10L);
        otp.setCardNumber("8600123412341234");
        otp.setCode("123456");
        otp.setStatus(OtpStatus.ACTIVE);
        otp.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        otp.setExpireAt(LocalDateTime.now().minusMinutes(1)); // expired

        when(otpRepository.findTopByUserIdAndCardNumberAndStatusOrderByCreatedAtDesc(
                10L, "8600123412341234", OtpStatus.ACTIVE
        )).thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> cardService.confirmCardAdding(
                new CardConfirmRequest(10L, "8600123412341234", "123456")
        ))
                .isInstanceOf(OtpExpiredException.class)
                .hasMessageContaining("OTP expired");

        verify(cardTxService).markOtpExpired(otpCaptor.capture());
        assertThat(otpCaptor.getValue().getStatus()).isEqualTo(OtpStatus.EXPIRED);

        verify(cardMapper, never()).newActiveCard(anyString(), any());
        verify(cardTxService, never()).createCardAndMarkOtpUsed(any(), any());
    }

    @Test
    void confirmCardAdding_whenOtpCodeWrong_throwsInvalidOtp_andDoesNotTouchTx() {
        OtpEntity otp = new OtpEntity();
        otp.setId(1L);
        otp.setUserId(10L);
        otp.setCardNumber("8600123412341234");
        otp.setCode("111111");
        otp.setStatus(OtpStatus.ACTIVE);
        otp.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        otp.setExpireAt(LocalDateTime.now().plusMinutes(1));

        when(otpRepository.findTopByUserIdAndCardNumberAndStatusOrderByCreatedAtDesc(
                10L, "8600123412341234", OtpStatus.ACTIVE
        )).thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> cardService.confirmCardAdding(
                new CardConfirmRequest(10L, "8600123412341234", "222222")
        ))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("Invalid OTP code");

        verifyNoInteractions(cardTxService, cardMapper);
        assertThat(otp.getStatus()).isEqualTo(OtpStatus.ACTIVE);
    }

    @Test
    void confirmCardAdding_whenCardAlreadyExists_throwsCardAlreadyExists() {
        OtpEntity otp = new OtpEntity();
        otp.setId(1L);
        otp.setUserId(10L);
        otp.setCardNumber("8600123412341234");
        otp.setCode("123456");
        otp.setStatus(OtpStatus.ACTIVE);
        otp.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        otp.setExpireAt(LocalDateTime.now().plusMinutes(1));

        when(otpRepository.findTopByUserIdAndCardNumberAndStatusOrderByCreatedAtDesc(
                10L, "8600123412341234", OtpStatus.ACTIVE
        )).thenReturn(Optional.of(otp));

        when(cardRepository.findByCardNumber("8600123412341234"))
                .thenReturn(Optional.of(new CardEntity()));

        assertThatThrownBy(() -> cardService.confirmCardAdding(
                new CardConfirmRequest(10L, "8600123412341234", "123456")
        ))
                .isInstanceOf(CardAlreadyExistsException.class)
                .hasMessageContaining("Card already exists");

        verifyNoInteractions(cardTxService, cardMapper);
        assertThat(otp.getStatus()).isEqualTo(OtpStatus.ACTIVE);
    }

    @Test
    void confirmCardAdding_happyPath_createsCard_marksOtpUsed_returnsMappedResponse() {
        OtpEntity otp = new OtpEntity();
        otp.setId(1L);
        otp.setUserId(10L);
        otp.setCardNumber("8600123412341234");
        otp.setCode("123456");
        otp.setStatus(OtpStatus.ACTIVE);
        otp.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        otp.setExpireAt(LocalDateTime.now().plusMinutes(1));

        when(otpRepository.findTopByUserIdAndCardNumberAndStatusOrderByCreatedAtDesc(
                10L, "8600123412341234", OtpStatus.ACTIVE
        )).thenReturn(Optional.of(otp));

        when(cardRepository.findByCardNumber("8600123412341234")).thenReturn(Optional.empty());

        LocalDateTime now = LocalDateTime.now();
        CardEntity newCard = new CardEntity();
        newCard.setCardNumber("8600123412341234");
        newCard.setBalance(BigDecimal.ZERO);
        newCard.setReservedBalance(BigDecimal.ZERO);
        newCard.setStatus(CardStatus.ACTIVE);
        newCard.setCreatedAt(now);
        newCard.setUpdatedAt(now);

        when(cardMapper.newActiveCard(eq("8600123412341234"), any(LocalDateTime.class)))
                .thenReturn(newCard);

        CardEntity savedCard = new CardEntity();
        savedCard.setId(100L);
        savedCard.setCardNumber("8600123412341234");
        savedCard.setBalance(BigDecimal.ZERO);
        savedCard.setReservedBalance(BigDecimal.ZERO);
        savedCard.setStatus(CardStatus.ACTIVE);
        savedCard.setCreatedAt(now);
        savedCard.setUpdatedAt(now);

        when(cardTxService.createCardAndMarkOtpUsed(any(CardEntity.class), any(OtpEntity.class)))
                .thenReturn(savedCard);

        CardConfirmResponse mapped = new CardConfirmResponse(
                "8600123412341234",
                CardStatus.ACTIVE.name(),
                now
        );
        when(cardMapper.toConfirmResponse(savedCard)).thenReturn(mapped);

        CardConfirmResponse response = cardService.confirmCardAdding(
                new CardConfirmRequest(10L, "8600123412341234", "123456")
        );

        assertThat(response).isNotNull();
        assertThat(response.cardNumber()).isEqualTo("8600123412341234");
        assertThat(response.status()).isEqualTo(CardStatus.ACTIVE.name());
        assertThat(response.createdAt()).isEqualTo(now);

        assertThat(otp.getStatus()).isEqualTo(OtpStatus.USED);

        verify(cardMapper).newActiveCard(eq("8600123412341234"), any(LocalDateTime.class));
        verify(cardTxService).createCardAndMarkOtpUsed(cardCaptor.capture(), otpCaptor.capture());
        assertThat(cardCaptor.getValue()).isSameAs(newCard);
        assertThat(otpCaptor.getValue()).isSameAs(otp);
        verify(cardMapper).toConfirmResponse(savedCard);

        verify(cardTxService, never()).markOtpExpired(any());
    }
}