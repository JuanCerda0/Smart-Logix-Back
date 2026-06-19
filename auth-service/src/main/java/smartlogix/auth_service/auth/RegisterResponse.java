package smartlogix.auth_service.auth;

public record RegisterResponse(
        String username,
        String tenant,
        String role
) {
}
