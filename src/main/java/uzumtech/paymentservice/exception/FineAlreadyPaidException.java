package uzumtech.paymentservice.exception;

public class FineAlreadyPaidException extends RuntimeException {
    public FineAlreadyPaidException(String message) {
        super(message);
    }
}
