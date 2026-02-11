package uzumtech.paymentservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uzumtech.paymentservice.dto.PaymentRequestDto;
import uzumtech.paymentservice.entity.PaymentEntity;
import uzumtech.paymentservice.mapper.PaymentMapper;
import uzumtech.paymentservice.repository.PaymentRepository;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    @Transactional
    public PaymentEntity createPayment(PaymentRequestDto request) {
        PaymentEntity entity = paymentMapper.toEntity(request);
        return paymentRepository.save(entity);
    }
}
