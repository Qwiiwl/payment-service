package uzumtech.paymentservice.service;

public interface OtpService {

    //Создает OTP и отправляет его либо на email, либо на телефон
    //На телефон смс не приходит, поэтому отправляем только по мылу
    Long createAndSendOtp(Long userId,
                          String cardNumber,
                          String phone,
                          String email);

    // Проверяет OTP.
    boolean verifyOtp(Long userId,
                      String cardNumber,
                      String code);
}