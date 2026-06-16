package smartlogix.auth_service.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    static final String TENANT_HEADER = "X-Tenant-Id";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(
            @RequestHeader(TENANT_HEADER) String tenant,
            @Valid @RequestBody RegisterRequest request
    ) {
        return authService.register(tenant, request);
    }

    @PostMapping("/login")
    public LoginResponse login(
            @RequestHeader(TENANT_HEADER) String tenant,
            @Valid @RequestBody LoginRequest request
    ) {
        return authService.login(tenant, request);
    }
}
