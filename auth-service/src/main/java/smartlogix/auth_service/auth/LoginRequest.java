package smartlogix.auth_service.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank
        @Size(max = 120)
        String username,

        @NotBlank
        @Size(max = 120)
        String password
) {
}
