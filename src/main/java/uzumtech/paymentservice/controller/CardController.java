package uzumtech.paymentservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uzumtech.paymentservice.dto.request.CardAddRequest;
import uzumtech.paymentservice.dto.request.CardConfirmRequest;
import uzumtech.paymentservice.dto.response.CardAddResponse;
import uzumtech.paymentservice.dto.response.CardConfirmResponse;
import uzumtech.paymentservice.service.CardService;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    /**
     * Шаг 1 — инициировать добавление карты (отправка OTP)
     */
    @PostMapping("/add")
    public ResponseEntity<CardAddResponse> initiateCardAdding(
            @RequestBody CardAddRequest request) {

        if (request.userId() == null || request.cardNumber() == null) {
            throw new IllegalArgumentException("UserId and cardNumber must not be null");
        }

        CardAddResponse response = cardService.initiateCardAdding(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Шаг 2 — подтверждение OTP и финальное создание карты
     */
    @PostMapping("/confirm")
    public ResponseEntity<CardConfirmResponse> confirmCardAdding(
            @RequestBody CardConfirmRequest request) {

        if (request.userId() == null ||
                request.cardNumber() == null ||
                request.otpCode() == null) {
            throw new IllegalArgumentException("UserId, cardNumber and otpCode must not be null");
        }

        CardConfirmResponse response = cardService.confirmCardAdding(request);
        return ResponseEntity.ok(response);
    }
}