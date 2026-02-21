package uzumtech.paymentservice.dto.request;

import java.math.BigDecimal;

public record TransferRequest(
        String fromCard,
        String toCard,
        BigDecimal amount
) { }
