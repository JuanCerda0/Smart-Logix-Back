package smartlogix.inventory_service.tenant;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "smartlogix.tenancy")
public class TenantProperties {

    private List<String> allowedTenants = List.of("empresa1", "empresa2");
    private String defaultTenant = "empresa1";

    public List<String> getAllowedTenants() {
        return allowedTenants;
    }

    public void setAllowedTenants(List<String> allowedTenants) {
        this.allowedTenants = allowedTenants;
    }

    public String getDefaultTenant() {
        return defaultTenant;
    }

    public void setDefaultTenant(String defaultTenant) {
        this.defaultTenant = defaultTenant;
    }

    public boolean isAllowed(String tenant) {
        return allowedTenants.stream().anyMatch(allowed -> allowed.equalsIgnoreCase(tenant));
    }
}
