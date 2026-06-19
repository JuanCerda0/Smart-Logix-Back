package smartlogix.BackendForFrontend.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void shouldLoginThroughAuthServiceForTenant() throws Exception {
        LoginRequest request = new LoginRequest("admin", "admin123");
        when(authService.login(eq("empresa1"), eq(request)))
                .thenReturn(new LoginResponse("token-value", "Bearer", 3600, "empresa1"));

        mockMvc.perform(post("/empresa1/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-value"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.tenant").value("empresa1"));
    }

    @Test
    void shouldRegisterThroughAuthServiceForTenant() throws Exception {
        RegisterRequest request = new RegisterRequest("new-user", "secret123");
        when(authService.register(eq("empresa2"), eq(request)))
                .thenReturn(new RegisterResponse("new-user", "empresa2", "USER"));

        mockMvc.perform(post("/empresa2/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "new-user",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new-user"))
                .andExpect(jsonPath("$.tenant").value("empresa2"))
                .andExpect(jsonPath("$.role").value("USER"));
    }
}
