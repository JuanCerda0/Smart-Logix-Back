package smartlogix.auth_service.auth;

public record TokenData(
        String token,
        long expiresIn
) {
}
