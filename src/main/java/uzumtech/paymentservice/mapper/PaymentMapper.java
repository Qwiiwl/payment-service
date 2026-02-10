package uzumtech.paymentservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uzumtech.paymentservice.domain.Payment;
import uzumtech.paymentservice.dto.PaymentRequestDto;
import uzumtech.paymentservice.dto.PaymentResponseDto;

import java.time.OffsetDateTime;

@Mapper(
        componentModel = "spring",
        imports = OffsetDateTime.class
)
public interface PaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "CREATED")
    @Mapping(
            target = "createdAt",
            expression = "java(OffsetDateTime.now())"
    )
    Payment toEntity(PaymentRequestDto dto);

    PaymentResponseDto toDto(Payment payment);
}
