package uzumtech.paymentservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import uzumtech.paymentservice.dto.response.CardConfirmResponse;
import uzumtech.paymentservice.entity.CardEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper(componentModel = "spring", imports = {BigDecimal.class})
public interface CardMapper {

    // создание энтити  
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cardNumber", expression = "java(cardNumber)") // всегда полный номер
    @Mapping(target = "balance", expression = "java(BigDecimal.ZERO)")
    @Mapping(target = "reservedBalance", expression = "java(BigDecimal.ZERO)")
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "createdAt", source = "now")
    @Mapping(target = "updatedAt", source = "now")
    CardEntity newActiveCard(String cardNumber, LocalDateTime now);


    // ответ энтити с маскированием номера 
    @Mapping(target = "status", expression = "java(card.getStatus().name())")
    @Mapping(target = "cardNumber", source = "cardNumber", qualifiedByName = "maskCardNumber")
    @Mapping(target = "createdAt", source = "createdAt")
    CardConfirmResponse toConfirmResponse(CardEntity card);


    // метод маскирования используется ТОЛЬКО в response
    @Named("maskCardNumber")
    default String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 10) {
            return cardNumber;
        }
        return cardNumber.substring(0, 4) + "****" + cardNumber.substring(cardNumber.length() - 4);
    }
}
