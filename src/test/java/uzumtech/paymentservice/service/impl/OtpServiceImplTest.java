package uzumtech.paymentservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import uzumtech.paymentservice.adapter.NotificationAdapter;
import uzumtech.paymentservice.constant.enums.OtpStatus;
import uzumtech.paymentservice.dto.request.NotificationSendRequest;
import uzumtech.paymentservice.entity.OtpEntity;
import uzumtech.paymentservice.repository.OtpRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

    @Mock
    OtpRepository otpRepository;

    @Mock
    NotificationAdapter notificationAdapter;

    @InjectMocks
    OtpServiceImpl otpService;

    @Captor
    ArgumentCaptor<NotificationSendRequest> requestCaptor;

    @Captor
    ArgumentCaptor<OtpEntity> otpCaptor;

    @BeforeEach
    void setup() {
        lenient().when(otpRepository.save(any())).thenAnswer(invocation -> {
            OtpEntity e = invocation.getArgument(0);
            if (e.getId() == null) e.setId(123L);
            return e;
        });
        lenient().when(notificationAdapter.send(any())).thenReturn(999L);
    }

    @Test
    void createAndSendOtp_emailChannel_sendsEmailAndSavesOtp() {
        // given
        Long userId = 1L;
        String email = "test@mail.com";

        // when
        otpService.createAndSendOtp(userId, null, null, email);

        // then: ушёл запрос в notificationAdapter
        verify(notificationAdapter).send(requestCaptor.capture());
        NotificationSendRequest req = requestCaptor.getValue();

        assertEquals("EMAIL", req.type());
        assertNotNull(req.receiver());
        assertEquals(email, req.receiver().email());
        assertNull(req.receiver().phone());

        // и OTP сохранился
        verify(otpRepository).save(otpCaptor.capture());
        OtpEntity saved = otpCaptor.getValue();

        assertNotNull(saved);
        assertEquals(userId, saved.getUserId());
        assertEquals(OtpStatus.ACTIVE, saved.getStatus()); // вместо NEW
        assertNotNull(saved.getCode());
        assertEquals(6, saved.getCode().length());
    }

    @Test
    void createAndSendOtp_smsChannel_sendsSmsAndSavesOtp() {
        Long userId = 1L;

        otpService.createAndSendOtp(userId, null, "+998901234567", null);

        verify(notificationAdapter).send(requestCaptor.capture());
        NotificationSendRequest req = requestCaptor.getValue();
        assertNotNull(req.type());
        assertTrue(req.type().equalsIgnoreCase("SMS"));
        assertNotNull(req.receiver().phone());
        assertTrue(req.receiver().phone().contains("998") || req.receiver().phone().startsWith("+"));
        assertNull(req.receiver().email());
    }

    @Test
    void createAndSendOtp_bothProvided_prefersEmail() {
        otpService.createAndSendOtp(1L, null, "+998901234567", "x@y.com");

        verify(notificationAdapter).send(requestCaptor.capture());
        assertEquals("EMAIL", requestCaptor.getValue().type());
    }

    @Test
    void verifyOtp_correctCode_marksUsedAndReturnsTrue() {
        Long userId = 1L;
        String code = "123456";

        OtpEntity active = OtpEntity.builder()
                .id(10L)
                .userId(userId)
                .cardNumber(null)
                .code(code)
                .status(OtpStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .expireAt(LocalDateTime.now().plusMinutes(4))
                .build();

        when(otpRepository.findTopByUserIdAndCardNumberAndStatusOrderByCreatedAtDesc(
                userId, null, OtpStatus.ACTIVE
        )).thenReturn(Optional.of(active));

        boolean ok = otpService.verifyOtp(userId, null, code);

        assertTrue(ok);
        assertEquals(OtpStatus.USED, active.getStatus());
        verify(otpRepository, atLeastOnce()).save(active);
    }

    @Test
    void verifyOtp_wrongCode_returnsFalse_doesNotMarkUsed() {
        Long userId = 1L;

        OtpEntity active = OtpEntity.builder()
                .id(10L)
                .userId(userId)
                .cardNumber(null)
                .code("111111")
                .status(OtpStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .expireAt(LocalDateTime.now().plusMinutes(4))
                .build();

        when(otpRepository.findTopByUserIdAndCardNumberAndStatusOrderByCreatedAtDesc(
                userId, null, OtpStatus.ACTIVE
        )).thenReturn(Optional.of(active));

        boolean ok = otpService.verifyOtp(userId, null, "222222");

        assertFalse(ok);
        assertEquals(OtpStatus.ACTIVE, active.getStatus());
        verify(otpRepository, never()).save(active);
    }

    @Test
    void verifyOtp_expired_marksExpiredAndThrows() {
        Long userId = 1L;

        OtpEntity active = OtpEntity.builder()
                .id(10L)
                .userId(userId)
                .cardNumber(null)
                .code("111111")
                .status(OtpStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .expireAt(LocalDateTime.now().minusSeconds(1))
                .build();

        when(otpRepository.findTopByUserIdAndCardNumberAndStatusOrderByCreatedAtDesc(
                userId, null, OtpStatus.ACTIVE
        )).thenReturn(Optional.of(active));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> otpService.verifyOtp(userId, null, "111111"));

        assertTrue(ex.getMessage().toLowerCase().contains("expired"));
        assertEquals(OtpStatus.EXPIRED, active.getStatus());
        verify(otpRepository).save(active);
    }
}