package uzumtech.paymentservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uzumtech.paymentservice.dto.request.PhoneTopUpRequest;
import uzumtech.paymentservice.dto.response.PhoneTopUpResponse;
import uzumtech.paymentservice.service.PhoneTopUpService;

@RestController
@RequestMapping("/phone-topup")
@RequiredArgsConstructor
public class PhoneTopUpController {

    private final PhoneTopUpService phoneTopUpService;

    @PostMapping
    public ResponseEntity<PhoneTopUpResponse> topUp(@Valid @RequestBody PhoneTopUpRequest request) {

        // Вызов сервиса
        PhoneTopUpResponse response = phoneTopUpService.topUp(request);

        // Возврат JSON
        return ResponseEntity.ok(response);
    }
}
