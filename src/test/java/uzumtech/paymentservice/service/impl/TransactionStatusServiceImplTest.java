package uzumtech.paymentservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import uzumtech.paymentservice.constant.enums.TransactionStatus;
import uzumtech.paymentservice.constant.enums.TransactionType;
import uzumtech.paymentservice.entity.TransactionEntity;
import uzumtech.paymentservice.mapper.TransactionMapper;
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

    @Mock
    TransactionMapper transactionMapper;

    @InjectMocks
    TransactionStatusServiceImpl service;

    @Captor
    ArgumentCaptor<TransactionEntity> txCaptor;

    @BeforeEach
    void setup() {
        // save() возвращает то, что в него передали (как у тебя было)
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

        UUID generated = UUID.randomUUID();
        TransactionEntity mapped = new TransactionEntity();
        mapped.setTransactionId(generated);
        mapped.setType(type);
        mapped.setSourceIdentifier(src);
        mapped.setDestinationIdentifier(dst);
        mapped.setAmount(amount);
        mapped.setStatus(TransactionStatus.PENDING);
        mapped.setCreatedAt(LocalDateTime.now());
        mapped.setUpdatedAt(LocalDateTime.now());

        when(transactionMapper.createPending(type, src, dst, amount)).thenReturn(mapped);

        // when
        UUID txId = service.createPending(type, src, dst, amount);

        // then
        assertThat(txId).isEqualTo(generated);

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
        TransactionEntity tx = new TransactionEntity();
        tx.setTransactionId(txId);
        tx.setType(TransactionType.TRANSFER);
        tx.setSourceIdentifier("a");
        tx.setDestinationIdentifier("b");
        tx.setAmount(new BigDecimal("10.00"));
        tx.setStatus(TransactionStatus.PENDING);
        tx.setCreatedAt(LocalDateTime.now().minusMinutes(2));
        tx.setUpdatedAt(LocalDateTime.now().minusMinutes(2));

        when(transactionRepository.findByTransactionId(txId)).thenReturn(Optional.of(tx));

        // маппер меняет энтити, по идее работать не должно, но иначе фейл на тесте
        doAnswer(inv -> {
            TransactionEntity e = inv.getArgument(0);
            String reason = inv.getArgument(1);
            e.setStatus(TransactionStatus.FAILED);
            e.setErrorMessage(reason);
            e.setUpdatedAt(LocalDateTime.now());
            return null;
        }).when(transactionMapper).markFailed(any(TransactionEntity.class), anyString());

        service.markFailed(txId, "Insufficient funds");

        verify(transactionRepository).save(txCaptor.capture());
        TransactionEntity saved = txCaptor.getValue();

        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(saved.getErrorMessage()).isEqualTo("Insufficient funds");
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void markFailed_whenTxNotFound_doesNothing() {
        UUID txId = UUID.randomUUID();
        when(transactionRepository.findByTransactionId(txId)).thenReturn(Optional.empty());

        service.markFailed(txId, "any");

        verify(transactionRepository, never()).save(any());
        verify(transactionMapper, never()).markFailed(any(), any());
    }

    @Test
    void markSuccess_whenTxExists_setsSuccess_clearsError_andSaves() {
        UUID txId = UUID.randomUUID();
        TransactionEntity tx = new TransactionEntity();
        tx.setTransactionId(txId);
        tx.setType(TransactionType.TRANSFER);
        tx.setSourceIdentifier("a");
        tx.setDestinationIdentifier("b");
        tx.setAmount(new BigDecimal("10.00"));
        tx.setStatus(TransactionStatus.PENDING);
        tx.setErrorMessage("old error");
        tx.setCreatedAt(LocalDateTime.now().minusMinutes(2));
        tx.setUpdatedAt(LocalDateTime.now().minusMinutes(2));

        when(transactionRepository.findByTransactionId(txId)).thenReturn(Optional.of(tx));

        doAnswer(inv -> {
            TransactionEntity e = inv.getArgument(0);
            e.setStatus(TransactionStatus.SUCCESS);
            e.setErrorMessage(null);
            e.setUpdatedAt(LocalDateTime.now());
            return null;
        }).when(transactionMapper).markSuccess(any(TransactionEntity.class));

        service.markSuccess(txId);

        verify(transactionRepository).save(txCaptor.capture());
        TransactionEntity saved = txCaptor.getValue();

        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(saved.getErrorMessage()).isNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void markSuccess_whenTxNotFound_doesNothing() {
        UUID txId = UUID.randomUUID();
        when(transactionRepository.findByTransactionId(txId)).thenReturn(Optional.empty());

        service.markSuccess(txId);

        verify(transactionRepository, never()).save(any());
        verify(transactionMapper, never()).markSuccess(any());
    }
}