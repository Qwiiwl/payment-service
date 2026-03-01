package uzumtech.paymentservice.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import uzumtech.paymentservice.constant.enums.TransactionType;
import uzumtech.paymentservice.dto.response.TransferResponse;
import uzumtech.paymentservice.exception.InsufficientFundsException;
import uzumtech.paymentservice.mapper.TransferMapper;
import uzumtech.paymentservice.service.BillingService;
import uzumtech.paymentservice.service.TransactionStatusService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock TransactionStatusService transactionStatusService;
    @Mock BillingService billingService;
    @Mock TransferMapper transferMapper;

    @InjectMocks TransferServiceImpl transferService;

    @Test
    void transfer_happyPath_marksSuccess_callsBilling_returnsMappedResponse() {
        String from = "8600000000000001";
        String to = "8600000000000002";
        BigDecimal amount = new BigDecimal("50.00");

        UUID txId = UUID.randomUUID();
        when(transactionStatusService.createPending(TransactionType.TRANSFER, from, to, amount))
                .thenReturn(txId);

        doNothing().when(billingService).transferBetweenCards(from, to, amount);

        TransferResponse mapped = new TransferResponse(
                txId, from, to, amount, "SUCCESS", LocalDateTime.now()
        );
        when(transferMapper.toResponse(eq(txId), eq(from), eq(to), eq(amount), eq("SUCCESS"), any(LocalDateTime.class)))
                .thenReturn(mapped);

        TransferResponse response = transferService.transfer(from, to, amount);

        assertThat(response).isSameAs(mapped);

        InOrder inOrder = inOrder(transactionStatusService, billingService, transferMapper);
        inOrder.verify(transactionStatusService).createPending(TransactionType.TRANSFER, from, to, amount);
        inOrder.verify(billingService).transferBetweenCards(from, to, amount);
        inOrder.verify(transactionStatusService).markSuccess(txId);
        inOrder.verify(transferMapper).toResponse(eq(txId), eq(from), eq(to), eq(amount), eq("SUCCESS"), any(LocalDateTime.class));

        verify(transactionStatusService, never()).markFailed(any(), any());
    }

    @Test
    void transfer_whenBillingThrows_marksFailedAndRethrows() {
        String from = "8600000000000001";
        String to = "8600000000000002";
        BigDecimal amount = new BigDecimal("50.00");

        UUID txId = UUID.randomUUID();
        when(transactionStatusService.createPending(TransactionType.TRANSFER, from, to, amount))
                .thenReturn(txId);

        RuntimeException ex = new InsufficientFundsException("Insufficient funds");
        doThrow(ex).when(billingService).transferBetweenCards(from, to, amount);

        assertThatThrownBy(() -> transferService.transfer(from, to, amount))
                .isSameAs(ex);

        verify(transactionStatusService).markFailed(eq(txId), eq("Insufficient funds"));
        verify(transactionStatusService, never()).markSuccess(any());
        verifyNoInteractions(transferMapper); // response не маппим, ибо падает
    }
}