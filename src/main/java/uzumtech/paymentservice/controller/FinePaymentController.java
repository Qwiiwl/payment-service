package uzumtech.paymentservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uzumtech.paymentservice.dto.request.FinePaymentRequest;
import uzumtech.paymentservice.dto.response.FinePaymentResponse;
import uzumtech.paymentservice.service.FinePaymentService;

@RestController
@RequestMapping("/fine-payment")
@RequiredArgsConstructor
public class FinePaymentController {

    private final FinePaymentService finePaymentService;

    @PostMapping
    public ResponseEntity<FinePaymentResponse> payFine(@Valid @RequestBody FinePaymentRequest request) {
        FinePaymentResponse response = finePaymentService.payFine(request);
        return ResponseEntity.ok(response);
    }
}
