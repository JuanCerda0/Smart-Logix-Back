package smartlogix.auth_service.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void shouldLoginSeededTenantAdmin() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .header(AuthController.TENANT_HEADER, "empresa1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(blankOrNullString())))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.tenant").value("empresa1"));
    }

    @Test
    void shouldIssueJwtWithTenantClaim() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .header(AuthController.TENANT_HEADER, "empresa1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");

        org.springframework.security.oauth2.jwt.Jwt jwt = jwtDecoder.decode(token);
        org.assertj.core.api.Assertions.assertThat(jwt.getClaimAsString("tenant")).isEqualTo("empresa1");
    }

    @Test
    void shouldRejectInvalidPassword() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .header(AuthController.TENANT_HEADER, "empresa1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "wrong"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRegisterUserForTenant() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .header(AuthController.TENANT_HEADER, "empresa2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "operator",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("operator"))
                .andExpect(jsonPath("$.tenant").value("empresa2"));
    }

    @Test
    void shouldRejectDuplicateUserInsideSameTenant() throws Exception {
        String body = """
                {
                  "username": "duplicate",
                  "password": "admin123"
                }
                """;

        mockMvc.perform(post("/auth/register")
                        .header(AuthController.TENANT_HEADER, "empresa1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                        .header(AuthController.TENANT_HEADER, "empresa1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectRegisterForUnknownTenant() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .header(AuthController.TENANT_HEADER, "tenant-missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "operator-missing",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectMissingTenantHeader() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .header(AuthController.TENANT_HEADER, "empresa1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
