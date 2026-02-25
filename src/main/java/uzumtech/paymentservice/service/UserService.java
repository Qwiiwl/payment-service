package uzumtech.paymentservice.service;

import uzumtech.paymentservice.dto.request.UserLoginRequest;
import uzumtech.paymentservice.dto.request.UserOtpConfirmRequest;
import uzumtech.paymentservice.dto.request.UserRegisterRequest;
import uzumtech.paymentservice.dto.response.OtpSentResponse;
import uzumtech.paymentservice.dto.response.UserResponse;

public interface UserService {

    OtpSentResponse register(UserRegisterRequest request);

    OtpSentResponse login(UserLoginRequest request);

    UserResponse confirmOtp(UserOtpConfirmRequest request);
}