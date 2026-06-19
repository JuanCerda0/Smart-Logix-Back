package smartlogix.BackendForFrontend.auth;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import smartlogix.BackendForFrontend.shared.ForbiddenException;

@Component
public class TenantAccessValidator {

    public void validate(String pathTenant, Jwt jwt) {
        String tokenTenant = jwt.getClaimAsString("tenant");
        if (tokenTenant == null || !pathTenant.equalsIgnoreCase(tokenTenant)) {
            throw new ForbiddenException("Token tenant does not match path tenant");
        }
    }
}
