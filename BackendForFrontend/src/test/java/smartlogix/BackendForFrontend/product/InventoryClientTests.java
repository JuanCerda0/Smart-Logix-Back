package smartlogix.BackendForFrontend.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class InventoryClientTests {

    private static final String BASE_URL = "https://inventory.internal";
    private static final String AUTHORIZATION = "Bearer token-value";
    private static final String TENANT = "empresa1";

    private MockRestServiceServer server;
    private InventoryClient inventoryClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        inventoryClient = new InventoryClient(builder.build());
    }

    @Test
    void shouldFindAllWithTenantAndAuthorizationHeaders() {
        server.expect(requestTo(BASE_URL + "/products"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(InventoryClient.TENANT_HEADER, TENANT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, AUTHORIZATION))
                .andRespond(withSuccess("""
                        [
                          {
                            "id": 1,
                            "sku": "SKU-001",
                            "name": "Product",
                            "description": "Description",
                            "category": "Testing",
                            "unitPrice": 9990,
                            "stock": 5,
                            "active": true
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        assertThat(inventoryClient.findAll(TENANT, AUTHORIZATION)).hasSize(1);
        server.verify();
    }

    @Test
    void shouldFindByIdWithExpectedRouteAndHeaders() {
        server.expect(requestTo(BASE_URL + "/products/7"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(InventoryClient.TENANT_HEADER, TENANT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, AUTHORIZATION))
                .andRespond(withSuccess(productResponseJson(7), MediaType.APPLICATION_JSON));

        ProductResponse response = inventoryClient.findById(TENANT, 7L, AUTHORIZATION);

        assertThat(response.id()).isEqualTo(7L);
        server.verify();
    }

    @Test
    void shouldCreateWithExpectedRouteHeadersAndBody() {
        ProductRequest request = productRequest();
        server.expect(requestTo(BASE_URL + "/products"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(InventoryClient.TENANT_HEADER, TENANT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, AUTHORIZATION))
                .andExpect(content().json(productRequestJson()))
                .andRespond(withSuccess(productResponseJson(8), MediaType.APPLICATION_JSON));

        ProductResponse response = inventoryClient.create(TENANT, request, AUTHORIZATION);

        assertThat(response.id()).isEqualTo(8L);
        server.verify();
    }

    @Test
    void shouldUpdateWithExpectedRouteHeadersAndBody() {
        ProductRequest request = productRequest();
        server.expect(requestTo(BASE_URL + "/products/9"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header(InventoryClient.TENANT_HEADER, TENANT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, AUTHORIZATION))
                .andExpect(content().json(productRequestJson()))
                .andRespond(withSuccess(productResponseJson(9), MediaType.APPLICATION_JSON));

        ProductResponse response = inventoryClient.update(TENANT, 9L, request, AUTHORIZATION);

        assertThat(response.id()).isEqualTo(9L);
        server.verify();
    }

    @Test
    void shouldUpdateStockWithExpectedRouteHeadersAndBody() {
        StockUpdateRequest request = new StockUpdateRequest(12);
        server.expect(requestTo(BASE_URL + "/products/10/stock"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header(InventoryClient.TENANT_HEADER, TENANT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, AUTHORIZATION))
                .andExpect(content().json("""
                        {"stock": 12}
                        """))
                .andRespond(withSuccess(productResponseJson(10), MediaType.APPLICATION_JSON));

        ProductResponse response = inventoryClient.updateStock(TENANT, 10L, request, AUTHORIZATION);

        assertThat(response.id()).isEqualTo(10L);
        server.verify();
    }

    @Test
    void shouldDeleteWithExpectedRouteAndHeaders() {
        server.expect(requestTo(BASE_URL + "/products/11"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header(InventoryClient.TENANT_HEADER, TENANT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, AUTHORIZATION))
                .andRespond(withNoContent());

        inventoryClient.delete(TENANT, 11L, AUTHORIZATION);

        server.verify();
    }

    private ProductRequest productRequest() {
        return new ProductRequest(
                "SKU-001",
                "Product",
                "Description",
                "Testing",
                BigDecimal.valueOf(9990),
                5);
    }

    private String productRequestJson() {
        return """
                {
                  "sku": "SKU-001",
                  "name": "Product",
                  "description": "Description",
                  "category": "Testing",
                  "unitPrice": 9990,
                  "stock": 5
                }
                """;
    }

    private String productResponseJson(long id) {
        return """
                {
                  "id": %d,
                  "sku": "SKU-001",
                  "name": "Product",
                  "description": "Description",
                  "category": "Testing",
                  "unitPrice": 9990,
                  "stock": 5,
                  "active": true
                }
                """.formatted(id);
    }
}
