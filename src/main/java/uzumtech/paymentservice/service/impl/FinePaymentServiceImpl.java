package uzumtech.paymentservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import uzumtech.paymentservice.dto.*;
import uzumtech.paymentservice.dto.request.FinePaymentRequest;
import uzumtech.paymentservice.dto.response.FinePaymentResponse;
import uzumtech.paymentservice.entity.*;
import uzumtech.paymentservice.constant.enums.*;
import uzumtech.paymentservice.exception.*;
import uzumtech.paymentservice.repository.*;
import uzumtech.paymentservice.service.FinePaymentService;
import uzumtech.paymentservice.service.tx.FinePaymentTxService;


@Service
@RequiredArgsConstructor
public class FinePaymentServiceImpl implements FinePaymentService {

    private final CardRepository cardRepository;
    private final FineRepository fineRepository;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    private final FinePaymentTxService finePaymentTxService;

    private static final String TOPIC = "transactions";

    @Override
    public FinePaymentResponse payFine(FinePaymentRequest request) {

        CardEntity fromCard = cardRepository.findByCardNumber(request.fromCard())
                .orElseThrow(() -> new CardNotFoundException("Card not found"));

        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardInactiveException("Card is not active");
        }

        FineEntity fine = fineRepository.findById(request.fineId())
                .orElseThrow(() -> new FineNotFoundException("Fine not found"));

        if (Boolean.TRUE.equals(fine.getPaid())) {
            throw new FineAlreadyPaidException("Fine already paid");
        }

        if (request.amount() == null || request.amount().compareTo(fine.getAmount()) != 0) {
            throw new IllegalArgumentException("Amount must equal fine amount");
        }

        if (fromCard.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds on card");
        }

        TransactionEntity transaction = finePaymentTxService.applyFinePayment(fromCard, fine, request.amount());

        return new FinePaymentResponse(
                transaction.getTransactionId(),
                fromCard.getCardNumber(),
                fine.getFineNumber(),
                request.amount(),
                transaction.getStatus().name(),
                transaction.getCreatedAt()
        );
    }
}