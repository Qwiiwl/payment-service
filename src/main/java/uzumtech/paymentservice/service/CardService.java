package uzumtech.paymentservice.service;

import uzumtech.paymentservice.dto.request.CardAddRequest;
import uzumtech.paymentservice.dto.request.CardConfirmRequest;
import uzumtech.paymentservice.dto.response.CardAddResponse;
import uzumtech.paymentservice.dto.response.CardConfirmResponse;

public interface CardService {

    CardAddResponse initiateCardAdding(CardAddRequest request);

    CardConfirmResponse confirmCardAdding(CardConfirmRequest request);
}