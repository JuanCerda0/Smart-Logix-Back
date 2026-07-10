package smartlogix.inventory_service.tenant;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TenantProperties.class)
public class TenantHibernateConfig {

    @Bean
    HibernatePropertiesCustomizer tenantHibernatePropertiesCustomizer(
            SchemaMultiTenantConnectionProvider connectionProvider,
            CurrentTenantResolver currentTenantResolver,
            TenantSchemaMapper tenantSchemaMapper
    ) {
        return properties -> {
            properties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
            properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantResolver);
            properties.put(AvailableSettings.MULTI_TENANT_SCHEMA_MAPPER, tenantSchemaMapper);
            properties.put("hibernate.multiTenancy", "SCHEMA");
        };
    }
}
