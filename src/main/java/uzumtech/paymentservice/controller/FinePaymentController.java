package uzumtech.paymentservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uzumtech.paymentservice.dto.request.FinePaymentRequest;
import uzumtech.paymentservice.dto.response.FinePaymentResponse;
import uzumtech.paymentservice.service.FinePaymentService;

import java.math.BigDecimal;

@RestController
@RequestMapping("/fine-payment")
@RequiredArgsConstructor
public class FinePaymentController {

    private final FinePaymentService finePaymentService;

    @PostMapping
    public ResponseEntity<FinePaymentResponse> payFine(@RequestBody FinePaymentRequest request) {

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        FinePaymentResponse response = finePaymentService.payFine(request);
        return ResponseEntity.ok(response);
    }
}
