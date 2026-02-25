package uzumtech.paymentservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uzumtech.paymentservice.dto.request.TransferRequest;
import uzumtech.paymentservice.dto.response.TransferResponse;
import uzumtech.paymentservice.service.TransferService;



@RestController
@RequestMapping("/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;


    @PostMapping
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {


        // Вызов сервиса
        TransferResponse response = transferService.transfer(
                request.fromCard(),
                request.toCard(),
                request.amount()
        );

        return ResponseEntity.ok(response);
    }
}

