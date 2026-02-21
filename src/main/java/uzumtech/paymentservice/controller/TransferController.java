package uzumtech.paymentservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uzumtech.paymentservice.dto.request.TransferRequest;
import uzumtech.paymentservice.dto.response.TransferResponse;
import uzumtech.paymentservice.entity.CardEntity;
import uzumtech.paymentservice.constant.enums.CardStatus;
import uzumtech.paymentservice.service.TransferService;
import uzumtech.paymentservice.repository.CardRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;
    private final CardRepository cardRepository;


    @PostMapping
    public ResponseEntity<TransferResponse> transfer(@RequestBody TransferRequest request) {
        // Проверка суммы
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        // Вызов сервиса
        TransferResponse response = transferService.transfer(
                request.fromCard(),
                request.toCard(),
                request.amount()
        );

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
