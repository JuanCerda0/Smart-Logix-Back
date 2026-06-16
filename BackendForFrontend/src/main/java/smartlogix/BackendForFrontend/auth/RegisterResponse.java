package smartlogix.BackendForFrontend.auth;

public record RegisterResponse(
        String username,
        String tenant,
        String role
) {
}
