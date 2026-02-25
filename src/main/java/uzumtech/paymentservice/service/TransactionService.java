package uzumtech.paymentservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uzumtech.paymentservice.constant.enums.TransactionType;
import uzumtech.paymentservice.dto.response.TransactionHistoryResponse;

public interface TransactionService {

    Page<TransactionHistoryResponse> getHistoryByCard(String cardNumber,
                                                      TransactionType type,
                                                      Pageable pageable);
}