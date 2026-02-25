package uzumtech.paymentservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import uzumtech.paymentservice.constant.enums.TransactionStatus;
import uzumtech.paymentservice.constant.enums.TransactionType;
import uzumtech.paymentservice.entity.TransactionEntity;
import uzumtech.paymentservice.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionStatusServiceImplTest {

    @Mock
    TransactionRepository transactionRepository;

    @InjectMocks
    TransactionStatusServiceImpl service;

    @Captor
    ArgumentCaptor<TransactionEntity> txCaptor;

    @BeforeEach
    void setup() {
        // save() возвращает то, что в него передали
        lenient().when(transactionRepository.save(any(TransactionEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createPending_savesPendingTransaction_andReturnsTxId() {
        // given
        TransactionType type = TransactionType.TRANSFER;
        String src = "8600_from";
        String dst = "8600_to";
        BigDecimal amount = new BigDecimal("12.34");

        // when
        UUID txId = service.createPending(type, src, dst, amount);

        // then
        assertThat(txId).isNotNull();

        verify(transactionRepository).save(txCaptor.capture());
        TransactionEntity saved = txCaptor.getValue();

        assertThat(saved.getTransactionId()).isEqualTo(txId);
        assertThat(saved.getType()).isEqualTo(type);
        assertThat(saved.getSourceIdentifier()).isEqualTo(src);
        assertThat(saved.getDestinationIdentifier()).isEqualTo(dst);
        assertThat(saved.getAmount()).isEqualByComparingTo(amount);
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void markFailed_whenTxExists_setsFailed_setsReason_andSaves() {
        // given
        UUID txId = UUID.randomUUID();
        TransactionEntity tx = TransactionEntity.builder()
                .transactionId(txId)
                .type(TransactionType.TRANSFER)
                .sourceIdentifier("a")
                .destinationIdentifier("b")
                .amount(new BigDecimal("10.00"))
                .status(TransactionStatus.PENDING)
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .updatedAt(LocalDateTime.now().minusMinutes(2))
                .build();

        when(transactionRepository.findByTransactionId(txId)).thenReturn(Optional.of(tx));

        // when
        service.markFailed(txId, "Insufficient funds");

        // then
        verify(transactionRepository).save(txCaptor.capture());
        TransactionEntity saved = txCaptor.getValue();

        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(saved.getErrorMessage()).isEqualTo("Insufficient funds");
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void markFailed_whenTxNotFound_doesNothing() {
        // given
        UUID txId = UUID.randomUUID();
        when(transactionRepository.findByTransactionId(txId)).thenReturn(Optional.empty());

        // when
        service.markFailed(txId, "any");

        // then
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void markSuccess_whenTxExists_setsSuccess_clearsError_andSaves() {
        // given
        UUID txId = UUID.randomUUID();
        TransactionEntity tx = TransactionEntity.builder()
                .transactionId(txId)
                .type(TransactionType.TRANSFER)
                .sourceIdentifier("a")
                .destinationIdentifier("b")
                .amount(new BigDecimal("10.00"))
                .status(TransactionStatus.PENDING)
                .errorMessage("old error")
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .updatedAt(LocalDateTime.now().minusMinutes(2))
                .build();

        when(transactionRepository.findByTransactionId(txId)).thenReturn(Optional.of(tx));

        // when
        service.markSuccess(txId);

        // then
        verify(transactionRepository).save(txCaptor.capture());
        TransactionEntity saved = txCaptor.getValue();

        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(saved.getErrorMessage()).isNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void markSuccess_whenTxNotFound_doesNothing() {
        // given
        UUID txId = UUID.randomUUID();
        when(transactionRepository.findByTransactionId(txId)).thenReturn(Optional.empty());

        // when
        service.markSuccess(txId);

        // then
        verify(transactionRepository, never()).save(any());
    }
}