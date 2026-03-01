package uzumtech.paymentservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uzumtech.paymentservice.constant.enums.TransactionType;
import uzumtech.paymentservice.dto.response.TransferResponse;
import uzumtech.paymentservice.mapper.TransferMapper;
import uzumtech.paymentservice.service.BillingService;
import uzumtech.paymentservice.service.TransferService;
import uzumtech.paymentservice.service.TransactionStatusService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

    @Service
    @RequiredArgsConstructor
    public class TransferServiceImpl implements TransferService {

    private final TransactionStatusService transactionStatusService;
    private final BillingService billingService;
    private final TransferMapper transferMapper;

    @Override
    public TransferResponse transfer(String fromCardNumber, String toCardNumber, BigDecimal amount) {

        UUID txId = transactionStatusService.createPending(
                TransactionType.TRANSFER,
                fromCardNumber,
                toCardNumber,
                amount
        );

        try {
            billingService.transferBetweenCards(fromCardNumber, toCardNumber, amount);
            transactionStatusService.markSuccess(txId);

            return transferMapper.toResponse(
                    txId,
                    fromCardNumber,
                    toCardNumber,
                    amount,
                    "SUCCESS",
                    LocalDateTime.now()
            );
        } catch (RuntimeException ex) {
            transactionStatusService.markFailed(txId, ex.getMessage());
            throw ex;
        }
    }
}