package smartlogix.BackendForFrontend.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.web.client.HttpClientErrorException;
import smartlogix.BackendForFrontend.auth.JwtTokenService;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

    private String empresa1Authorization;

    @BeforeEach
    void setUp() {
        empresa1Authorization = authorizationFor("empresa1");
    }

    @Test
    void shouldRejectEveryProductEndpointWithoutJwt() throws Exception {
        mockMvc.perform(productRequest(HttpMethod.GET, "/empresa1/api/products", null))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(productRequest(HttpMethod.GET, "/empresa1/api/products/10", null))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(productRequest(HttpMethod.POST, "/empresa1/api/products", null))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(productRequest(HttpMethod.PUT, "/empresa1/api/products/10", null))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(productRequest(HttpMethod.PATCH, "/empresa1/api/products/10/stock", null))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(productRequest(HttpMethod.DELETE, "/empresa1/api/products/10", null))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(inventoryClient);
    }

    @Test
    void shouldRejectEveryProductEndpointWhenPathTenantDoesNotMatchJwtTenant() throws Exception {
        mockMvc.perform(productRequest(HttpMethod.GET, "/empresa2/api/products", empresa1Authorization))
                .andExpect(status().isForbidden());
        mockMvc.perform(productRequest(HttpMethod.GET, "/empresa2/api/products/10", empresa1Authorization))
                .andExpect(status().isForbidden());
        mockMvc.perform(productRequest(HttpMethod.POST, "/empresa2/api/products", empresa1Authorization))
                .andExpect(status().isForbidden());
        mockMvc.perform(productRequest(HttpMethod.PUT, "/empresa2/api/products/10", empresa1Authorization))
                .andExpect(status().isForbidden());
        mockMvc.perform(productRequest(HttpMethod.PATCH, "/empresa2/api/products/10/stock", empresa1Authorization))
                .andExpect(status().isForbidden());
        mockMvc.perform(productRequest(HttpMethod.DELETE, "/empresa2/api/products/10", empresa1Authorization))
                .andExpect(status().isForbidden());

        verifyNoInteractions(inventoryClient);
    }

    @Test
    void shouldFindAllProductsWithMatchingTenantJwtAndForwardAuthorization() throws Exception {
        when(inventoryClient.findAll(anyString(), anyString())).thenReturn(List.of());

        mockMvc.perform(get("/empresa1/api/products")
                        .header("Authorization", empresa1Authorization))
                .andExpect(status().isOk());

        verify(inventoryClient).findAll(eq("empresa1"), eq(empresa1Authorization));
    }

    @Test
    void shouldFindProductByIdWithMatchingTenantJwtAndForwardAuthorization() throws Exception {
        when(inventoryClient.findById(anyString(), eq(10L), anyString())).thenReturn(productResponse(10L));

        mockMvc.perform(get("/empresa1/api/products/10")
                        .header("Authorization", empresa1Authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));

        verify(inventoryClient).findById(eq("empresa1"), eq(10L), eq(empresa1Authorization));
    }

    @Test
    void shouldCreateProductWithMatchingTenantJwtAndForwardAuthorization() throws Exception {
        ProductRequest request = validProductRequest();
        when(inventoryClient.create(eq("empresa1"), eq(request), eq(empresa1Authorization))).thenReturn(productResponse(11L));

        mockMvc.perform(productRequest(HttpMethod.POST, "/empresa1/api/products", empresa1Authorization))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11));

        verify(inventoryClient).create(eq("empresa1"), eq(request), eq(empresa1Authorization));
    }

    @Test
    void shouldUpdateProductWithMatchingTenantJwtAndForwardAuthorization() throws Exception {
        ProductRequest request = validProductRequest();
        when(inventoryClient.update(eq("empresa1"), eq(12L), eq(request), eq(empresa1Authorization))).thenReturn(productResponse(12L));

        mockMvc.perform(productRequest(HttpMethod.PUT, "/empresa1/api/products/12", empresa1Authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12));

        verify(inventoryClient).update(eq("empresa1"), eq(12L), eq(request), eq(empresa1Authorization));
    }

    @Test
    void shouldUpdateStockWithMatchingTenantJwtAndForwardAuthorization() throws Exception {
        StockUpdateRequest request = new StockUpdateRequest(7);
        when(inventoryClient.updateStock(eq("empresa1"), eq(13L), eq(request), eq(empresa1Authorization))).thenReturn(productResponse(13L));

        mockMvc.perform(productRequest(HttpMethod.PATCH, "/empresa1/api/products/13/stock", empresa1Authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(13));

        verify(inventoryClient).updateStock(eq("empresa1"), eq(13L), eq(request), eq(empresa1Authorization));
    }

    @Test
    void shouldDeleteProductWithMatchingTenantJwtAndForwardAuthorization() throws Exception {
        mockMvc.perform(productRequest(HttpMethod.DELETE, "/empresa1/api/products/14", empresa1Authorization))
                .andExpect(status().isNoContent());

        verify(inventoryClient).delete(eq("empresa1"), eq(14L), eq(empresa1Authorization));
    }

    @Test
    void shouldRejectInvalidProductBodiesWithBadRequest() throws Exception {
        String[] invalidBodies = {
                productJson("", "Valid Name", "Testing", "9990", "1"),
                productJson("SKU-001", "", "Testing", "9990", "1"),
                productJson("SKU-001", "Valid Name", "", "9990", "1"),
                productJson("SKU-001", "Valid Name", "Testing", "0", "1"),
                productJson("SKU-001", "Valid Name", "Testing", "9990", "-1"),
                productJson("SKU-001", "Valid Name", "Testing", "9990", "null")
        };

        for (String invalidBody : invalidBodies) {
            mockMvc.perform(request(HttpMethod.POST, "/empresa1/api/products")
                            .header("Authorization", empresa1Authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }

        verifyNoInteractions(inventoryClient);
    }

    @Test
    void shouldRejectInvalidStockBodiesWithBadRequest() throws Exception {
        String[] invalidBodies = {
                """
                {"stock": -1}
                """,
                """
                {"stock": null}
                """
        };

        for (String invalidBody : invalidBodies) {
            mockMvc.perform(request(HttpMethod.PATCH, "/empresa1/api/products/10/stock")
                            .header("Authorization", empresa1Authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }

        verifyNoInteractions(inventoryClient);
    }

    @Test
    void shouldPropagateDownstreamErrorStatuses() throws Exception {
        when(inventoryClient.findById(eq("empresa1"), eq(401L), eq(empresa1Authorization)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null));
        when(inventoryClient.findById(eq("empresa1"), eq(403L), eq(empresa1Authorization)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden", null, null, null));
        when(inventoryClient.findById(eq("empresa1"), eq(404L), eq(empresa1Authorization)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));
        when(inventoryClient.findById(eq("empresa1"), eq(409L), eq(empresa1Authorization)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.CONFLICT, "Conflict", null, null, null));

        mockMvc.perform(get("/empresa1/api/products/401").header("Authorization", empresa1Authorization))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/empresa1/api/products/403").header("Authorization", empresa1Authorization))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/empresa1/api/products/404").header("Authorization", empresa1Authorization))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/empresa1/api/products/409").header("Authorization", empresa1Authorization))
                .andExpect(status().isConflict());
    }

    private RequestBuilder productRequest(HttpMethod method, String path, String authorizationHeader) {
        var builder = request(method, path).contentType(MediaType.APPLICATION_JSON);
        if (authorizationHeader != null) {
            builder.header("Authorization", authorizationHeader);
        }
        if (HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method)) {
            builder.content(productJson("SKU-001", "Valid Name", "Testing", "9990", "5"));
        } else if (HttpMethod.PATCH.equals(method)) {
            builder.content("""
                    {"stock": 7}
                    """);
        }
        return builder;
    }

    private String authorizationFor(String tenant) {
        return "Bearer " + jwtTokenService.generateToken("admin", tenant).token();
    }

    private ProductRequest validProductRequest() {
        return new ProductRequest(
                "SKU-001",
                "Valid Name",
                "Created from test",
                "Testing",
                BigDecimal.valueOf(9990),
                5);
    }

    private ProductResponse productResponse(Long id) {
        return new ProductResponse(
                id,
                "SKU-001",
                "Valid Name",
                "Created from test",
                "Testing",
                BigDecimal.valueOf(9990),
                5,
                true,
                null,
                null);
    }

    private String productJson(String sku, String name, String category, String unitPrice, String stock) {
        return """
                {
                  "sku": "%s",
                  "name": "%s",
                  "description": "Created from test",
                  "category": "%s",
                  "unitPrice": %s,
                  "stock": %s
                }
                """.formatted(sku, name, category, unitPrice, stock);
    }
}
