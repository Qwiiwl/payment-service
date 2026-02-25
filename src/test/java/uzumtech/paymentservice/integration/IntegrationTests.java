//package uzumtech.paymentservice.integration;
//
//import org.apache.kafka.clients.consumer.Consumer;
//import org.apache.kafka.clients.consumer.ConsumerRecords;
//import org.apache.kafka.clients.consumer.KafkaConsumer;
//import org.apache.kafka.common.serialization.StringDeserializer;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.test.EmbeddedKafkaBroker;
//import org.springframework.kafka.test.context.EmbeddedKafka;
//import org.springframework.kafka.test.utils.KafkaTestUtils;
//import org.springframework.kafka.support.serializer.JsonDeserializer;
//import org.springframework.transaction.annotation.Transactional;
//import uzumtech.paymentservice.config.TestKafkaConfig;
//import uzumtech.paymentservice.dto.request.FinePaymentRequest;
//import uzumtech.paymentservice.dto.request.PhoneTopUpRequest;
//import uzumtech.paymentservice.dto.TransactionEvent;
//import uzumtech.paymentservice.entity.CardEntity;
//import uzumtech.paymentservice.entity.FineEntity;
//import uzumtech.paymentservice.constant.enums.CardStatus;
//import uzumtech.paymentservice.exception.*;
//import uzumtech.paymentservice.repository.CardRepository;
//import uzumtech.paymentservice.repository.FineRepository;
//import uzumtech.paymentservice.service.FinePaymentService;
//import uzumtech.paymentservice.service.PhoneTopUpService;
//import uzumtech.paymentservice.service.TransferService;
//
//import java.math.BigDecimal;
//import java.time.Duration;
//import java.util.Map;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//
//@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
//@EmbeddedKafka(partitions = 1, topics = {"transactions"})
//@Import(TestKafkaConfig.class)
//@Transactional
//class IntegrationTests {
//
//    @Autowired
//    private TransferService transferService;
//
//    @Autowired
//    private FinePaymentService finePaymentService;
//
//    @Autowired
//    private PhoneTopUpService phoneTopUpService;
//
//    @Autowired
//    private CardRepository cardRepository;
//
//    @Autowired
//    private FineRepository fineRepository;
//
//    @Autowired
//    private EmbeddedKafkaBroker embeddedKafka;
//
//    @Autowired
//    private KafkaTemplate<String, TransactionEvent> kafkaTemplate;
//
//    private CardEntity card1;
//    private CardEntity card2;
//    private FineEntity fine;
//
//    @BeforeEach
//    void setUp() {
//        card1 = CardEntity.builder()
//                .cardNumber("1111222233334444")
//                .balance(new BigDecimal("1000"))
//                .status(CardStatus.ACTIVE)
//                .build();
//
//        card2 = CardEntity.builder()
//                .cardNumber("5555666677778888")
//                .balance(new BigDecimal("500"))
//                .status(CardStatus.ACTIVE)
//                .build();
//
//        cardRepository.save(card1);
//        cardRepository.save(card2);
//
//        fine = FineEntity.builder()
//                .fineNumber("FINE-5")
//                .amount(new BigDecimal("300"))
//                .paid(false)
//                .build();
//
//        fineRepository.save(fine);
//    }
//
//    @Test
//    void testAllPaymentsAndKafkaEvents() {
//
//        transferService.transfer(card1.getCardNumber(), card2.getCardNumber(), new BigDecimal("200"));
//
//        assertThat(cardRepository.findByCardNumber(card1.getCardNumber()).get().getBalance())
//                .isEqualByComparingTo("800");
//
//        assertThat(cardRepository.findByCardNumber(card2.getCardNumber()).get().getBalance())
//                .isEqualByComparingTo("700");
//
//        FinePaymentRequest fineRequest = new FinePaymentRequest();
//        fineRequest.setFromCard(card1.getCardNumber());
//        fineRequest.setFineId(fine.getId());
//        fineRequest.setAmount(fine.getAmount());
//
//        finePaymentService.payFine(fineRequest);
//
//        assertThat(fineRepository.findById(fine.getId()).get().getPaid()).isTrue();
//
//        assertThat(cardRepository.findByCardNumber(card1.getCardNumber()).get().getBalance())
//                .isEqualByComparingTo("500");
//
//        PhoneTopUpRequest topUpRequest = new PhoneTopUpRequest();
//        topUpRequest.setFromCard(card1.getCardNumber());
//        topUpRequest.setPhoneNumber("+998901112233");
//        topUpRequest.setAmount(new BigDecimal("100"));
//
//        phoneTopUpService.topUp(topUpRequest);
//
//        assertThat(cardRepository.findByCardNumber(card1.getCardNumber()).get().getBalance())
//                .isEqualByComparingTo("400");
//
//        Map<String, Object> consumerProps =
//                KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafka);
//
//        consumerProps.put("key.deserializer", StringDeserializer.class.getName());
//        consumerProps.put("value.deserializer", JsonDeserializer.class.getName());
//        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
//        consumerProps.put("auto.offset.reset", "earliest");
//
//        try (Consumer<String, TransactionEvent> consumer =
//                     new KafkaConsumer<>(consumerProps)) {
//
//            embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "transactions");
//
//            ConsumerRecords<String, TransactionEvent> records =
//                    KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));
//
//            assertThat(records.count()).isGreaterThanOrEqualTo(3);
//        }
//    }
//
//    @Test
//    void testNegativeScenarios_noKafkaOnFailure() {
//
//        assertThrows(InsufficientFundsException.class, () ->
//                transferService.transfer(card2.getCardNumber(), card1.getCardNumber(), new BigDecimal("1000")));
//
//        FinePaymentRequest fineRequest = new FinePaymentRequest();
//        fineRequest.setFromCard(card1.getCardNumber());
//        fineRequest.setFineId(fine.getId());
//        fineRequest.setAmount(fine.getAmount());
//
//        finePaymentService.payFine(fineRequest);
//
//        assertThrows(FineAlreadyPaidException.class, () ->
//                finePaymentService.payFine(fineRequest));
//
//        card1.setStatus(CardStatus.BLOCKED);
//        cardRepository.save(card1);
//
//        PhoneTopUpRequest topUpRequest = new PhoneTopUpRequest();
//        topUpRequest.setFromCard(card1.getCardNumber());
//        topUpRequest.setPhoneNumber("+998901112233");
//        topUpRequest.setAmount(new BigDecimal("50"));
//
//        assertThrows(CardInactiveException.class,
//                () -> phoneTopUpService.topUp(topUpRequest));
//    }
//}
