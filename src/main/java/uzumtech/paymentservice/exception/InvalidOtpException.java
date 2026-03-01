package uzumtech.paymentservice.exception;

public class InvalidOtpException extends RuntimeException {

    public InvalidOtpException(String message) {
        super(message);
    }
}