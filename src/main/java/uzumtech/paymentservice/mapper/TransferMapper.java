package uzumtech.paymentservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uzumtech.paymentservice.dto.response.TransferResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface TransferMapper {

    // тхИД + входные данные -> респонс
    @Mapping(target = "transactionId", source = "txId")
    @Mapping(target = "fromCard", source = "fromCard")
    @Mapping(target = "toCard", source = "toCard")
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", source = "createdAt")
    TransferResponse toResponse(
            UUID txId,
            String fromCard,
            String toCard,
            BigDecimal amount,
            String status,
            LocalDateTime createdAt
    );

    //маскировать номера карт в ответах
    default String mask(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 10) return cardNumber;
        return cardNumber.substring(0, 4) + "****" + cardNumber.substring(cardNumber.length() - 4);
    }
}