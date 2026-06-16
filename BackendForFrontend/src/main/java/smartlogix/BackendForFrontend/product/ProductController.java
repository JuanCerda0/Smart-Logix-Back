package smartlogix.BackendForFrontend.product;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import smartlogix.BackendForFrontend.auth.TenantAccessValidator;

import java.util.List;

@RestController
@RequestMapping("/{tenant}/api/products")
public class ProductController {

    private final InventoryClient inventoryClient;
    private final TenantAccessValidator tenantAccessValidator;

    public ProductController(InventoryClient inventoryClient, TenantAccessValidator tenantAccessValidator) {
        this.inventoryClient = inventoryClient;
        this.tenantAccessValidator = tenantAccessValidator;
    }

    @GetMapping
    public List<ProductResponse> findAll(
            @PathVariable String tenant,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @AuthenticationPrincipal Jwt jwt
    ) {
        tenantAccessValidator.validate(tenant, jwt);
        return inventoryClient.findAll(tenant, authorizationHeader);
    }

    @GetMapping("/{id}")
    public ProductResponse findById(
            @PathVariable String tenant,
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @AuthenticationPrincipal Jwt jwt
    ) {
        tenantAccessValidator.validate(tenant, jwt);
        return inventoryClient.findById(tenant, id, authorizationHeader);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(
            @PathVariable String tenant,
            @Valid @RequestBody ProductRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @AuthenticationPrincipal Jwt jwt
    ) {
        tenantAccessValidator.validate(tenant, jwt);
        return inventoryClient.create(tenant, request, authorizationHeader);
    }

    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable String tenant,
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @AuthenticationPrincipal Jwt jwt
    ) {
        tenantAccessValidator.validate(tenant, jwt);
        return inventoryClient.update(tenant, id, request, authorizationHeader);
    }

    @PatchMapping("/{id}/stock")
    public ProductResponse updateStock(
            @PathVariable String tenant,
            @PathVariable Long id,
            @Valid @RequestBody StockUpdateRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @AuthenticationPrincipal Jwt jwt
    ) {
        tenantAccessValidator.validate(tenant, jwt);
        return inventoryClient.updateStock(tenant, id, request, authorizationHeader);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable String tenant,
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @AuthenticationPrincipal Jwt jwt
    ) {
        tenantAccessValidator.validate(tenant, jwt);
        inventoryClient.delete(tenant, id, authorizationHeader);
    }
}
