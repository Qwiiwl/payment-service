package uzumtech.paymentservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uzumtech.paymentservice.dto.PaymentRequestDto;
import uzumtech.paymentservice.dto.PaymentResponseDto;
import uzumtech.paymentservice.mapper.PaymentMapper;
import uzumtech.paymentservice.service.PaymentService;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    @PostMapping
    public PaymentResponseDto create(@RequestBody @Valid PaymentRequestDto request) {
        var payment = paymentMapper.toEntity(request);
        var saved = paymentService.create(payment);
        return paymentMapper.toDto(saved);
    }
}
