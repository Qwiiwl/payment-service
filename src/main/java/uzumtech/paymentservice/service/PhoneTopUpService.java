package uzumtech.paymentservice.service;

import uzumtech.paymentservice.dto.request.PhoneTopUpRequest;
import uzumtech.paymentservice.dto.response.PhoneTopUpResponse;

public interface PhoneTopUpService {

    PhoneTopUpResponse topUp(PhoneTopUpRequest request);

}
