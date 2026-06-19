package smartlogix.auth_service.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByTenantSlugIgnoreCaseAndUsernameIgnoreCase(String tenantSlug, String username);

    boolean existsByTenantSlugIgnoreCaseAndUsernameIgnoreCase(String tenantSlug, String username);
}
