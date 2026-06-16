package smartlogix.inventory_service.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class CurrentTenantResolver implements CurrentTenantIdentifierResolver<String> {

    private final TenantProperties tenantProperties;

    public CurrentTenantResolver(TenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenant = TenantContext.getTenant();
        return tenant == null ? tenantProperties.getDefaultTenant() : tenant;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
