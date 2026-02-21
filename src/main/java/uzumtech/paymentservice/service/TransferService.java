package uzumtech.paymentservice.service;

import uzumtech.paymentservice.dto.response.TransferResponse;

import java.math.BigDecimal;

public interface TransferService {

    TransferResponse transfer(String fromCardNumber,
                              String toCardNumber,
                              BigDecimal amount);

}
