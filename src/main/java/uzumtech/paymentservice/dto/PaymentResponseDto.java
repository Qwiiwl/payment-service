package uzumtech.paymentservice.dto;

import lombok.Builder;
import lombok.Value;
import uzumtech.paymentservice.domain.PaymentStatus;
import uzumtech.paymentservice.domain.PaymentType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder
public class PaymentResponseDto {

    UUID id;
    PaymentType type;
    PaymentStatus status;
    BigDecimal amount;
    String currency;
    String account;
    OffsetDateTime createdAt;
}
