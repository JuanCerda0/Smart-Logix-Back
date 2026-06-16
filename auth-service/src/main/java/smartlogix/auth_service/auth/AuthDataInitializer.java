package smartlogix.auth_service.auth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AuthDataInitializer implements CommandLineRunner {

    private final TenantRepository tenantRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthDataInitializer(
            TenantRepository tenantRepository,
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.tenantRepository = tenantRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedTenantWithAdmin("empresa1", "Empresa 1");
        seedTenantWithAdmin("empresa2", "Empresa 2");
    }

    private void seedTenantWithAdmin(String slug, String name) {
        Tenant tenant = tenantRepository.findBySlugIgnoreCase(slug)
                .orElseGet(() -> tenantRepository.save(new Tenant(slug, name)));

        if (!userAccountRepository.existsByTenantSlugIgnoreCaseAndUsernameIgnoreCase(slug, "admin")) {
            userAccountRepository.save(new UserAccount(
                    tenant,
                    "admin",
                    passwordEncoder.encode("admin123"),
                    "TENANT_ADMIN"));
        }
    }
}
