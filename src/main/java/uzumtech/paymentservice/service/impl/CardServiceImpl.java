package uzumtech.paymentservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements uzumtech.paymentservice.service.CardService {

    private final CardRepository cardRepository;
    private final OtpRepository otpRepository;

    private static final int OTP_EXPIRATION_MINUTES = 3;

    /**
     * Шаг 1 — отправка OTP
     */
    @Override
    @Transactional
    public CardAddResponse initiateCardAdding(CardAddRequest request) {

        // Проверка что карта уже не существует
        if (cardRepository.findByCardNumber(request.cardNumber()).isPresent()) {
            throw new CardAlreadyExistsException("Card already exists");
        }

        // Генерация 6-значного OTP
        String otpCode = generateOtp();

        // Создание записи OTP
        OtpEntity otp = OtpEntity.builder()
                .userId(request.userId())
                .cardNumber(request.cardNumber())
                .code(otpCode)
                .status(OtpStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .expireAt(LocalDateTime.now().plusMinutes(OTP_EXPIRATION_MINUTES))
                .build();

        otpRepository.save(otp);

        // TODO: здесь должен быть вызов J-Notification
        // notificationService.sendOtp(...)

        return new CardAddResponse("OTP_SENT");
    }

    /**
     * Шаг 2 — подтверждение OTP и создание карты
     */
    @Override
    @Transactional
    public CardConfirmResponse confirmCardAdding(CardConfirmRequest request) {

        OtpEntity otp = otpRepository
                .findTopByUserIdAndCardNumberAndStatusOrderByCreatedAtDesc(
                        request.userId(),
                        request.cardNumber(),
                        OtpStatus.ACTIVE
                )
                .orElseThrow(() -> new InvalidOtpException("OTP not found"));

        // Проверка срока действия
        if (otp.getExpireAt().isBefore(LocalDateTime.now())) {
            otp.setStatus(OtpStatus.EXPIRED);
            otpRepository.save(otp);
            throw new OtpExpiredException("OTP expired");
        }

        // Проверка кода
        if (!otp.getCode().equals(request.otpCode())) {
            throw new InvalidOtpException("Invalid OTP code");
        }

        // Проверка повторного создания карты
        if (cardRepository.findByCardNumber(request.cardNumber()).isPresent()) {
            throw new CardAlreadyExistsException("Card already exists");
        }

        // Создание карты
        CardEntity card = CardEntity.builder()
                .cardNumber(request.cardNumber())
                .balance(java.math.BigDecimal.ZERO)
                .status(CardStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        cardRepository.save(card);

        // Помечаем OTP как использованный
        otp.setStatus(OtpStatus.USED);
        otpRepository.save(otp);

        return new CardConfirmResponse(
                card.getCardNumber(),
                card.getStatus().name(),
                card.getCreatedAt()
        );
    }

    private String generateOtp() {
        Random random = new Random();
        int number = 100000 + random.nextInt(900000);
        return String.valueOf(number);
    }
}