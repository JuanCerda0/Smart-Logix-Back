package smartlogix.auth_service.auth;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        String tenant
) {
}
