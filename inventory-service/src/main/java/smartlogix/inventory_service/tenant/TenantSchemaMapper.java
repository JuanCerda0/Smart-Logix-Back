package smartlogix.inventory_service.tenant;

import org.springframework.stereotype.Component;

@Component
public class TenantSchemaMapper implements org.hibernate.context.spi.TenantSchemaMapper<String> {

    @Override
    public String schemaName(String tenantIdentifier) {
        return tenantIdentifier;
    }
}
