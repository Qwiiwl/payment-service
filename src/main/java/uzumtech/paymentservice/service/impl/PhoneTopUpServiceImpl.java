package uzumtech.paymentservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import uzumtech.paymentservice.dto.*;
import uzumtech.paymentservice.dto.request.PhoneTopUpRequest;
import uzumtech.paymentservice.dto.response.PhoneTopUpResponse;
import uzumtech.paymentservice.entity.*;
import uzumtech.paymentservice.constant.enums.*;
import uzumtech.paymentservice.exception.*;
import uzumtech.paymentservice.repository.*;
import uzumtech.paymentservice.service.PhoneTopUpService;
import uzumtech.paymentservice.service.tx.PhoneTopUpTxService;


@Service
@RequiredArgsConstructor
public class PhoneTopUpServiceImpl implements PhoneTopUpService {

    private final CardRepository cardRepository;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    private final PhoneTopUpTxService phoneTopUpTxService;

    private static final String TOPIC = "transactions";

    @Override
    public PhoneTopUpResponse topUp(PhoneTopUpRequest request) {

        CardEntity fromCard = cardRepository.findByCardNumber(request.fromCard())
                .orElseThrow(() -> new CardNotFoundException("Card not found"));

        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardInactiveException("Card is not active");
        }

        if (fromCard.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds on card");
        }

        TransactionEntity transaction = phoneTopUpTxService.applyTopUp(fromCard, request.phoneNumber(), request.amount());

        return new PhoneTopUpResponse(
                transaction.getTransactionId(),
                fromCard.getCardNumber(),
                request.phoneNumber(),
                request.amount(),
                transaction.getStatus().name(),
                transaction.getCreatedAt()
        );
    }
}
