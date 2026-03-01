package uzumtech.paymentservice.controller;

import jakarta.validation.Valid;
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



     //инициировать добавление карты
    @PostMapping("/add")
    public ResponseEntity<CardAddResponse> initiateCardAdding(
            @Valid @RequestBody CardAddRequest request) {

        CardAddResponse response = cardService.initiateCardAdding(request);
        return ResponseEntity.ok(response);
    }


    //подтверждение отп и финальное создание карты
    @PostMapping("/confirm")
    public ResponseEntity<CardConfirmResponse> confirmCardAdding(
            @Valid @RequestBody CardConfirmRequest request) {

        CardConfirmResponse response = cardService.confirmCardAdding(request);
        return ResponseEntity.ok(response);
    }
}
