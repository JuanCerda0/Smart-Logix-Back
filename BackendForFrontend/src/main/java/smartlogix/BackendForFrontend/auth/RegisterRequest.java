package smartlogix.BackendForFrontend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(max = 120)
        String username,

        @NotBlank
        @Size(min = 6, max = 120)
        String password
) {
}
