package uzumtech.paymentservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import uzumtech.paymentservice.constant.enums.CardStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NotNull private Long id;

    @Column(name = "card_number", nullable = false, unique = true)
    @NotBlank private String cardNumber;

    @Column(nullable = false)
    @PositiveOrZero private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotBlank private CardStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(nullable = false)
    @PositiveOrZero private BigDecimal reservedBalance = BigDecimal.ZERO;
}
