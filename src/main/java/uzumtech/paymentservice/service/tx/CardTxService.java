package uzumtech.paymentservice.service.tx;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uzumtech.paymentservice.entity.CardEntity;
import uzumtech.paymentservice.entity.OtpEntity;
import uzumtech.paymentservice.repository.CardRepository;
import uzumtech.paymentservice.repository.OtpRepository;


 //транзакции держим только вокруг сохранений
@Component
@RequiredArgsConstructor
public class CardTxService {

    private final CardRepository cardRepository;
    private final OtpRepository otpRepository;

    @Transactional
    public void saveOtp(OtpEntity otp) {
        otpRepository.save(otp);
    }

    @Transactional
    public CardEntity createCardAndMarkOtpUsed(CardEntity card, OtpEntity otp) {
        CardEntity savedCard = cardRepository.save(card);
        otpRepository.save(otp);
        return savedCard;
    }

    @Transactional
    public void markOtpExpired(OtpEntity otp) {
        otpRepository.save(otp);
    }
}
