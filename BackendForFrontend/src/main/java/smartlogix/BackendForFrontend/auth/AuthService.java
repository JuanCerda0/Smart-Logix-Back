package smartlogix.BackendForFrontend.auth;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AuthService {

    static final String TENANT_HEADER = "X-Tenant-Id";

    private final RestClient authRestClient;

    public AuthService(@Qualifier("authRestClient") RestClient authRestClient) {
        this.authRestClient = authRestClient;
    }

    public LoginResponse login(String tenant, LoginRequest request) {
        return authRestClient.post()
                .uri("/auth/login")
                .header(TENANT_HEADER, tenant)
                .body(request)
                .retrieve()
                .body(LoginResponse.class);
    }

    public RegisterResponse register(String tenant, RegisterRequest request) {
        return authRestClient.post()
                .uri("/auth/register")
                .header(TENANT_HEADER, tenant)
                .body(request)
                .retrieve()
                .body(RegisterResponse.class);
    }
}
