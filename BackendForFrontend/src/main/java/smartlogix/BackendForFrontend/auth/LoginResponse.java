package smartlogix.BackendForFrontend.auth;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        String tenant
) {
}
