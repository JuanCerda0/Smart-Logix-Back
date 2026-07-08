package smartlogix.BackendForFrontend.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AuthServiceTests {

    private static final String BASE_URL = "https://auth.internal";
    private static final String TENANT = "empresa1";

    private MockRestServiceServer server;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        authService = new AuthService(builder.build());
    }

    @Test
    void shouldLoginUsingInternalAuthRouteAndTenantHeader() {
        LoginRequest request = new LoginRequest("admin", "admin123");
        server.expect(requestTo(BASE_URL + "/auth/login"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(AuthService.TENANT_HEADER, TENANT))
                .andExpect(content().json("""
                        {
                          "username": "admin",
                          "password": "admin123"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "token": "token-value",
                          "tokenType": "Bearer",
                          "expiresIn": 3600,
                          "tenant": "empresa1"
                        }
                        """, MediaType.APPLICATION_JSON));

        LoginResponse response = authService.login(TENANT, request);

        assertThat(response.token()).isEqualTo("token-value");
        assertThat(response.tenant()).isEqualTo(TENANT);
        server.verify();
    }

    @Test
    void shouldRegisterUsingInternalAuthRouteAndTenantHeader() {
        RegisterRequest request = new RegisterRequest("operator", "secret123");
        server.expect(requestTo(BASE_URL + "/auth/register"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(AuthService.TENANT_HEADER, TENANT))
                .andExpect(content().json("""
                        {
                          "username": "operator",
                          "password": "secret123"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "username": "operator",
                          "tenant": "empresa1",
                          "role": "TENANT_ADMIN"
                        }
                        """, MediaType.APPLICATION_JSON));

        RegisterResponse response = authService.register(TENANT, request);

        assertThat(response.username()).isEqualTo("operator");
        assertThat(response.tenant()).isEqualTo(TENANT);
        server.verify();
    }
}
