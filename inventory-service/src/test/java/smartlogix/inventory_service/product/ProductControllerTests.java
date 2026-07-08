package smartlogix.inventory_service.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTests {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTenantSchemas() {
        jdbcTemplate.execute("DELETE FROM empresa1.products");
        jdbcTemplate.execute("DELETE FROM empresa2.products");
    }

    @Test
    void shouldRejectProductsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/products")
                        .header(TENANT_HEADER, "empresa1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectProductsWithoutTenantHeader() throws Exception {
        mockMvc.perform(get("/products")
                        .with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCreateAndListProductsForTenant() throws Exception {
        productRepository.deleteAll();

        mockMvc.perform(post("/products")
                        .with(jwt())
                        .header(TENANT_HEADER, "empresa1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "SKU-TEST-001",
                                  "name": "Test Product",
                                  "description": "Created from test",
                                  "category": "Testing",
                                  "unitPrice": 9990,
                                  "stock": 12
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("SKU-TEST-001"))
                .andExpect(jsonPath("$.stock").value(12));

        mockMvc.perform(get("/products")
                        .with(jwt())
                        .header(TENANT_HEADER, "empresa1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sku").value("SKU-TEST-001"));
    }

    @Test
    void shouldUpdateProductStock() throws Exception {
        productRepository.deleteAll();
        Product product = productRepository.save(new Product(
                "SKU-STOCK-001",
                "Stock Product",
                "Product used for stock update test",
                "Testing",
                java.math.BigDecimal.valueOf(14990),
                5));

        mockMvc.perform(patch("/products/{id}/stock", product.getId())
                        .with(jwt())
                        .header(TENANT_HEADER, "empresa1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stock": 18
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(18));
    }

    @Test
    void shouldReturnNotFoundForMissingProduct() throws Exception {
        mockMvc.perform(get("/products/{id}", 99999)
                        .with(jwt())
                        .header(TENANT_HEADER, "empresa1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateProductCompletely() throws Exception {
        productRepository.deleteAll();
        Product product = productRepository.save(new Product(
                "SKU-UPDATE-001",
                "Original Product",
                "Original description",
                "Testing",
                java.math.BigDecimal.valueOf(14990),
                5));

        mockMvc.perform(put("/products/{id}", product.getId())
                        .with(jwt())
                        .header(TENANT_HEADER, "empresa1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "SKU-UPDATE-002",
                                  "name": "Updated Product",
                                  "description": "Updated description",
                                  "category": "Updated",
                                  "unitPrice": 25990,
                                  "stock": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-UPDATE-002"))
                .andExpect(jsonPath("$.name").value("Updated Product"))
                .andExpect(jsonPath("$.category").value("Updated"))
                .andExpect(jsonPath("$.unitPrice").value(25990))
                .andExpect(jsonPath("$.stock").value(20));
    }

    @Test
    void shouldDeleteProduct() throws Exception {
        productRepository.deleteAll();
        Product product = productRepository.save(new Product(
                "SKU-DELETE-001",
                "Delete Product",
                "Product used for delete test",
                "Testing",
                java.math.BigDecimal.valueOf(14990),
                5));

        mockMvc.perform(delete("/products/{id}", product.getId())
                        .with(jwt())
                        .header(TENANT_HEADER, "empresa1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/products/{id}", product.getId())
                        .with(jwt())
                        .header(TENANT_HEADER, "empresa1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectInvalidProductRequest() throws Exception {
        mockMvc.perform(post("/products")
                        .with(jwt())
                        .header(TENANT_HEADER, "empresa1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "",
                                  "name": "",
                                  "description": "Invalid product",
                                  "category": "",
                                  "unitPrice": 0,
                                  "stock": -1
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectNegativeStockUpdate() throws Exception {
        productRepository.deleteAll();
        Product product = productRepository.save(new Product(
                "SKU-STOCK-NEG-001",
                "Stock Product",
                "Product used for stock validation test",
                "Testing",
                java.math.BigDecimal.valueOf(14990),
                5));

        mockMvc.perform(patch("/products/{id}/stock", product.getId())
                        .with(jwt())
                        .header(TENANT_HEADER, "empresa1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stock": -1
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldIsolateProductsBetweenTenants() throws Exception {
        productRepository.deleteAll();

        String productJson = """
                {
                  "sku": "SKU-SHARED-001",
                  "name": "Shared SKU Product",
                  "description": "Same SKU in different tenants",
                  "category": "Testing",
                  "unitPrice": 19990,
                  "stock": 7
                }
                """;

        mockMvc.perform(post("/products")
                        .with(jwt())
                        .header(TENANT_HEADER, "empresa1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/products")
                        .with(jwt())
                        .header(TENANT_HEADER, "empresa2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/products")
                        .with(jwt())
                        .header(TENANT_HEADER, "empresa1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sku").value("SKU-SHARED-001"));

        mockMvc.perform(get("/products")
                        .with(jwt())
                        .header(TENANT_HEADER, "empresa2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sku").value("SKU-SHARED-001"));
    }

    @Test
    void shouldRejectDuplicateSkuInsideSameTenant() throws Exception {
        productRepository.deleteAll();

        String productJson = """
                {
                  "sku": "SKU-DUP-001",
                  "name": "Duplicate Product",
                  "description": "Duplicate SKU in same tenant",
                  "category": "Testing",
                  "unitPrice": 9990,
                  "stock": 4
                }
                """;

        mockMvc.perform(post("/products")
                        .with(jwt())
                        .header(TENANT_HEADER, "empresa1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/products")
                        .with(jwt())
                        .header(TENANT_HEADER, "empresa1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson))
                .andExpect(status().isConflict());
    }
}
