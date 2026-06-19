package smartlogix.BackendForFrontend.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import smartlogix.BackendForFrontend.auth.JwtTokenService;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private InventoryClient inventoryClient;

    @Test
    void shouldRejectProductsWithoutJwt() throws Exception {
        mockMvc.perform(get("/empresa1/api/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowProductsWithMatchingTenantJwt() throws Exception {
        when(inventoryClient.findAll(anyString(), anyString())).thenReturn(List.of());
        String token = jwtTokenService.generateToken("admin", "empresa1").token();

        mockMvc.perform(get("/empresa1/api/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        verify(inventoryClient).findAll(eq("empresa1"), eq("Bearer " + token));
    }

    @Test
    void shouldRejectProductsWhenPathTenantDoesNotMatchJwtTenant() throws Exception {
        String token = jwtTokenService.generateToken("admin", "empresa1").token();

        mockMvc.perform(get("/empresa2/api/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
