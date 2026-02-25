package uzumtech.paymentservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uzumtech.paymentservice.adapter.NotificationAdapter;
import uzumtech.paymentservice.constant.enums.OtpStatus;
import uzumtech.paymentservice.dto.request.NotificationSendRequest;
import uzumtech.paymentservice.entity.OtpEntity;
import uzumtech.paymentservice.repository.OtpRepository;
import uzumtech.paymentservice.service.OtpService;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final OtpRepository otpRepository;
    private final NotificationAdapter notificationAdapter;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_TTL_MINUTES = 5;

    // CREATE + SEND OTP
    @Override
    @Transactional
    public Long createAndSendOtp(Long userId,
                                 String cardNumber,
                                 String phone,
                                 String email) {

        String normalizedPhone = normalize(phone);
        String normalizedEmail = normalize(email);

        Channel channel = resolveChannel(normalizedPhone, normalizedEmail);

        String code = generateOtp();

        LocalDateTime now = LocalDateTime.now();

        OtpEntity otp = OtpEntity.builder()
                .userId(userId)
                .cardNumber(cardNumber)
                .code(code)
                .status(OtpStatus.ACTIVE)
                .createdAt(now)
                .expireAt(now.plusMinutes(OTP_TTL_MINUTES))
                .build();

        otp = otpRepository.save(otp);

        sendNotification(channel, normalizedPhone, normalizedEmail, code);

        return otp.getId();
    }

    // VERIFY OTP
    @Override
    @Transactional
    public boolean verifyOtp(Long userId,
                             String cardNumber,
                             String code) {

        OtpEntity otp = otpRepository
                .findTopByUserIdAndCardNumberAndStatusOrderByCreatedAtDesc(
                        userId,
                        cardNumber,
                        OtpStatus.ACTIVE
                )
                .orElseThrow(() -> new IllegalArgumentException("OTP not found"));

        if (otp.getExpireAt().isBefore(LocalDateTime.now())) {
            otp.setStatus(OtpStatus.EXPIRED);
            otpRepository.save(otp);
            throw new IllegalStateException("OTP expired");
        }

        if (!otp.getCode().equals(code)) {
            return false;
        }

        otp.setStatus(OtpStatus.USED);
        otpRepository.save(otp);

        return true;
    }

    // SEND NOTIFICATION
    private void sendNotification(Channel channel,
                                  String phone,
                                  String email,
                                  String code) {

        String text = "Your OTP code: " + code;

        NotificationSendRequest request = switch (channel) {
            case EMAIL -> new NotificationSendRequest(
                    new NotificationSendRequest.Receiver(null, email, null),
                    "EMAIL",
                    text
            );
            case SMS -> new NotificationSendRequest(
                    new NotificationSendRequest.Receiver(phone, null, null),
                    "SMS",
                    text
            );
        };

        notificationAdapter.send(request);
    }

    // HELPERS
    private String generateOtp() {
        int value = RANDOM.nextInt(1_000_000);
        return String.format("%06d", value);
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Channel resolveChannel(String phone, String email) {

        boolean hasPhone = phone != null;
        boolean hasEmail = email != null;

        if (hasPhone && hasEmail) {
            return Channel.EMAIL;
        }

        if (hasEmail) return Channel.EMAIL;
        if (hasPhone) return Channel.SMS;

        throw new IllegalArgumentException("Provide phone or email");
    }

    private enum Channel {
        EMAIL,
        SMS
    }
}