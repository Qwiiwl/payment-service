package uzumtech.paymentservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uzumtech.paymentservice.dto.request.UserLoginRequest;
import uzumtech.paymentservice.dto.request.UserOtpConfirmRequest;
import uzumtech.paymentservice.dto.request.UserRegisterRequest;
import uzumtech.paymentservice.dto.response.OtpSentResponse;
import uzumtech.paymentservice.dto.response.UserResponse;
import uzumtech.paymentservice.service.UserService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<OtpSentResponse> register(@RequestBody @Valid UserRegisterRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<OtpSentResponse> login(@RequestBody @Valid UserLoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/confirm")
    public ResponseEntity<UserResponse> confirm(@RequestBody @Valid UserOtpConfirmRequest request) {
        return ResponseEntity.ok(userService.confirmOtp(request));
    }
}