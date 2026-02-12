package uzumtech.paymentservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uzumtech.paymentservice.dto.PhoneTopUpRequest;
import uzumtech.paymentservice.dto.PhoneTopUpResponse;
import uzumtech.paymentservice.service.PhoneTopUpService;

import java.math.BigDecimal;

@RestController
@RequestMapping("/phone-topup")
@RequiredArgsConstructor
public class PhoneTopUpController {

    private final PhoneTopUpService phoneTopUpService;

    @PostMapping
    public ResponseEntity<PhoneTopUpResponse> topUp(@RequestBody PhoneTopUpRequest request) {

        // Проверка корректности суммы
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        // Вызов сервиса
        PhoneTopUpResponse response = phoneTopUpService.topUp(request);

        // Возврат корректного JSON
        return ResponseEntity.ok(response);
    }
}
