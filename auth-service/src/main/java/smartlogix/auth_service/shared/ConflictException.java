package smartlogix.auth_service.shared;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
