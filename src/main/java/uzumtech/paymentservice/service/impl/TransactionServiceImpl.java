package uzumtech.paymentservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uzumtech.paymentservice.constant.enums.TransactionType;
import uzumtech.paymentservice.dto.response.TransactionHistoryResponse;
import uzumtech.paymentservice.entity.TransactionEntity;
import uzumtech.paymentservice.mapper.TransactionMapper;
import uzumtech.paymentservice.repository.TransactionRepository;
import uzumtech.paymentservice.service.TransactionService;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Override
    public Page<TransactionHistoryResponse> getHistoryByCard(String cardNumber,
                                                             TransactionType type,
                                                             Pageable pageable) {

        Page<TransactionEntity> page;

        if (type == null) {
            page = transactionRepository.findBySourceIdentifierOrDestinationIdentifier(
                    cardNumber, cardNumber, pageable
            );
        } else {
            page = transactionRepository.findByTypeAndSourceIdentifierOrTypeAndDestinationIdentifier(
                    type, cardNumber, type, cardNumber, pageable
            );
        }

        return page.map(transactionMapper::toHistoryResponse);
    }
}