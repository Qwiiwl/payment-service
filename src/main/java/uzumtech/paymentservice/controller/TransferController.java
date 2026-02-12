package uzumtech.paymentservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uzumtech.paymentservice.dto.TransferRequest;
import uzumtech.paymentservice.dto.TransferResponse;
import uzumtech.paymentservice.entity.CardEntity;
import uzumtech.paymentservice.entity.enums.CardStatus;
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

    /**
     * POST /transfer
     * Тело запроса JSON:
     * {
     *   "fromCard": "1234567890123456",
     *   "toCard": "6543210987654321",
     *   "amount": 100.50
     * }
     */
    @PostMapping
    public ResponseEntity<TransferResponse> transfer(@RequestBody TransferRequest request) {
        // Проверка суммы
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        // Вызов сервиса
        TransferResponse response = transferService.transfer(
                request.getFromCard(),
                request.getToCard(),
                request.getAmount()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * GET для просмотра всех карт (тестовый endpoint)
     */
    @GetMapping("/cards/test")
    public List<CardEntity> getAllCards() {
        return cardRepository.findAll();
    }

    /**
     * Временный endpoint для создания тестовой карты
     * Пример запроса: POST /transfer/cards/test?number=1111222233334444&balance=1000
     */
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
