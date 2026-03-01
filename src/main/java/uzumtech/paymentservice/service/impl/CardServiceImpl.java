package uzumtech.paymentservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
import uzumtech.paymentservice.mapper.CardMapper;
import uzumtech.paymentservice.repository.CardRepository;
import uzumtech.paymentservice.repository.OtpRepository;
import uzumtech.paymentservice.repository.UserRepository;
import uzumtech.paymentservice.service.CardService;
import uzumtech.paymentservice.service.OtpService;
import uzumtech.paymentservice.service.tx.CardTxService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final OtpRepository otpRepository;

    //отп на мыло
    private final UserRepository userRepository;
    private final OtpService otpService;

    private final CardTxService cardTxService;
    private final CardMapper cardMapper;

    @Override
    public CardAddResponse initiateCardAdding(CardAddRequest request) {

        if (cardRepository.findByCardNumber(request.cardNumber()).isPresent()) {
            throw new CardAlreadyExistsException("Card already exists");
        }

        String email = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getEmail();

        otpService.createAndSendOtp(
                request.userId(),
                request.cardNumber(),
                null,
                email
        );

        return new CardAddResponse("OTP sent to email for card adding");
    }

    @Override
    public CardConfirmResponse confirmCardAdding(CardConfirmRequest request) {

        OtpEntity otp = otpRepository
                .findTopByUserIdAndCardNumberAndStatusOrderByCreatedAtDesc(
                        request.userId(),
                        request.cardNumber(),
                        OtpStatus.ACTIVE
                )
                .orElseThrow(() -> new InvalidOtpException("OTP not found"));

        if (otp.getExpireAt().isBefore(LocalDateTime.now())) {
            otp.setStatus(OtpStatus.EXPIRED);
            cardTxService.markOtpExpired(otp);
            throw new OtpExpiredException("OTP expired");
        }

        if (!otp.getCode().equals(request.otpCode())) {
            throw new InvalidOtpException("Invalid OTP code");
        }

        if (cardRepository.findByCardNumber(request.cardNumber()).isPresent()) {
            throw new CardAlreadyExistsException("Card already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        CardEntity card = cardMapper.newActiveCard(request.cardNumber(), now);

        otp.setStatus(OtpStatus.USED);
        CardEntity saved = cardTxService.createCardAndMarkOtpUsed(card, otp);

        return cardMapper.toConfirmResponse(saved);
    }
}