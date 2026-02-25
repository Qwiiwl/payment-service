package uzumtech.paymentservice.dto.response;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String fullName,
        String phoneNumber,
        LocalDateTime createdAt
) {}