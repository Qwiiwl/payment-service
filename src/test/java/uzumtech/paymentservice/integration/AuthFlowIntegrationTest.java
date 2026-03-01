package uzumtech.paymentservice.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import uzumtech.paymentservice.adapter.NotificationAdapter;
import uzumtech.paymentservice.constant.enums.OtpStatus;
import uzumtech.paymentservice.dto.request.UserOtpConfirmRequest;
import uzumtech.paymentservice.dto.request.UserRegisterRequest;
import uzumtech.paymentservice.dto.response.OtpSentResponse;
import uzumtech.paymentservice.dto.response.UserResponse;
import uzumtech.paymentservice.entity.OtpEntity;
import uzumtech.paymentservice.repository.OtpRepository;
import uzumtech.paymentservice.repository.UserRepository;
import uzumtech.paymentservice.service.UserService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class AuthFlowIntegrationTest extends IntegrationTestBase {

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    OtpRepository otpRepository;

    @MockBean
    NotificationAdapter notificationAdapter;

    @Test
    @Transactional
    void register_createsOtp_confirm_marksUsed() {
        when(notificationAdapter.send(any())).thenReturn(1L);

        String phone = "+998900000001";
        String email = "test1@mail.com";

        OtpSentResponse sent = userService.register(new UserRegisterRequest(
                "Arvi Test",
                phone,
                "123456",
                email
        ));

        assertNotNull(sent);
        assertTrue(sent.message().toLowerCase().contains("otp"));

        // OTP должен появиться в БД
        Long userId = userRepository.findByPhoneNumber(phone).orElseThrow().getId();

        Optional<OtpEntity> opt = otpRepository
                .findTopByUserIdAndCardNumberAndStatusOrderByCreatedAtDesc(userId, null, OtpStatus.ACTIVE);

        assertTrue(opt.isPresent());
        OtpEntity otp = opt.get();
        assertNotNull(otp.getCode());

        // confirm
        UserResponse resp = userService.confirmOtp(new UserOtpConfirmRequest(phone, otp.getCode()));
        assertNotNull(resp);
        assertEquals(phone, resp.phoneNumber());

        // OTP стал USED
        OtpEntity updated = otpRepository.findById(otp.getId()).orElseThrow();
        assertEquals(OtpStatus.USED, updated.getStatus());
    }
}