package uzumtech.paymentservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uzumtech.paymentservice.constant.enums.CardStatus;
import uzumtech.paymentservice.dto.request.CardAddRequest;
import uzumtech.paymentservice.dto.request.CardConfirmRequest;
import uzumtech.paymentservice.dto.response.CardAddResponse;
import uzumtech.paymentservice.dto.response.CardConfirmResponse;
import uzumtech.paymentservice.entity.CardEntity;
import uzumtech.paymentservice.service.CardService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import uzumtech.paymentservice.repository.CardRepository;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    private final CardRepository cardRepository;



     //инициировать добавление карты (отправка OTP)
    @PostMapping("/add")
    public ResponseEntity<CardAddResponse> initiateCardAdding(
            @RequestBody CardAddRequest request) {



        CardAddResponse response = cardService.initiateCardAdding(request);
        return ResponseEntity.ok(response);
    }


    //подтверждение OTP и финальное создание карты
    @PostMapping("/confirm")
    public ResponseEntity<CardConfirmResponse> confirmCardAdding(
            @RequestBody CardConfirmRequest request) {



        CardConfirmResponse response = cardService.confirmCardAdding(request);
        return ResponseEntity.ok(response);
    }

    //GET для просмотра всех карт
    @GetMapping("/cards/test")
    public List<CardEntity> getAllCards() {
        return cardRepository.findAll();
    }

    //Временный endpoint для создания тестовой карты
    @PostMapping("/cards/test")
    public CardEntity createTestCard(@RequestParam String number, @RequestParam BigDecimal balance) {
        CardEntity card = CardEntity.builder()
                .cardNumber(number)
                .balance(balance)
                .status(CardStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return cardRepository.save(card);
    }
}
