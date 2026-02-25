package uzumtech.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import uzumtech.paymentservice.constant.enums.OtpStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_codes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // После добавления юзеров изменить связь на @ManyToOne
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Временно храним номер карты, которую хотят добавить
    @Column(name = "card_number", nullable = false, length = 32)
    private String cardNumber;

    @Column(name = "code", nullable = false, length = 6)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OtpStatus status;

    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}