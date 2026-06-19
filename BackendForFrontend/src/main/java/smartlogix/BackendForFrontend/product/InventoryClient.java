package smartlogix.BackendForFrontend.product;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class InventoryClient {

    static final String TENANT_HEADER = "X-Tenant-Id";

    private final RestClient inventoryRestClient;

    public InventoryClient(@Qualifier("inventoryRestClient") RestClient inventoryRestClient) {
        this.inventoryRestClient = inventoryRestClient;
    }

    public List<ProductResponse> findAll(String tenant, String authorizationHeader) {
        return inventoryRestClient.get()
                .uri("/products")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .header(TENANT_HEADER, tenant)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public ProductResponse findById(String tenant, Long id, String authorizationHeader) {
        return inventoryRestClient.get()
                .uri("/products/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .header(TENANT_HEADER, tenant)
                .retrieve()
                .body(ProductResponse.class);
    }

    public ProductResponse create(String tenant, ProductRequest request, String authorizationHeader) {
        return inventoryRestClient.post()
                .uri("/products")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .header(TENANT_HEADER, tenant)
                .body(request)
                .retrieve()
                .body(ProductResponse.class);
    }

    public ProductResponse update(String tenant, Long id, ProductRequest request, String authorizationHeader) {
        return inventoryRestClient.put()
                .uri("/products/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .header(TENANT_HEADER, tenant)
                .body(request)
                .retrieve()
                .body(ProductResponse.class);
    }

    public ProductResponse updateStock(String tenant, Long id, StockUpdateRequest request, String authorizationHeader) {
        return inventoryRestClient.patch()
                .uri("/products/{id}/stock", id)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .header(TENANT_HEADER, tenant)
                .body(request)
                .retrieve()
                .body(ProductResponse.class);
    }

    public void delete(String tenant, Long id, String authorizationHeader) {
        inventoryRestClient.delete()
                .uri("/products/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .header(TENANT_HEADER, tenant)
                .retrieve()
                .toBodilessEntity();
    }
}
