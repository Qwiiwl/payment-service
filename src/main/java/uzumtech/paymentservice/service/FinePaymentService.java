package uzumtech.paymentservice.service;

import uzumtech.paymentservice.dto.request.FinePaymentRequest;
import uzumtech.paymentservice.dto.response.FinePaymentResponse;

public interface FinePaymentService {

    FinePaymentResponse payFine(FinePaymentRequest request);

}
