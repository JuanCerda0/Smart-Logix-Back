package smartlogix.auth_service.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import smartlogix.auth_service.shared.ConflictException;
import smartlogix.auth_service.shared.NotFoundException;
import smartlogix.auth_service.shared.UnauthorizedException;

@Service
@Transactional
public class AuthService {

    private static final String DEFAULT_ROLE = "TENANT_ADMIN";

    private final TenantRepository tenantRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(
            TenantRepository tenantRepository,
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this.tenantRepository = tenantRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    public RegisterResponse register(String tenantSlug, RegisterRequest request) {
        Tenant tenant = findTenant(tenantSlug);
        if (userAccountRepository.existsByTenantSlugIgnoreCaseAndUsernameIgnoreCase(tenantSlug, request.username())) {
            throw new ConflictException("Username already exists for tenant: " + tenantSlug);
        }

        UserAccount account = new UserAccount(
                tenant,
                request.username(),
                passwordEncoder.encode(request.password()),
                DEFAULT_ROLE);

        UserAccount saved = userAccountRepository.save(account);
        return new RegisterResponse(saved.getUsername(), tenant.getSlug(), saved.getRole());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(String tenantSlug, LoginRequest request) {
        UserAccount account = userAccountRepository
                .findByTenantSlugIgnoreCaseAndUsernameIgnoreCase(tenantSlug, request.username())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        TokenData tokenData = jwtTokenService.generateToken(account);
        return new LoginResponse(tokenData.token(), "Bearer", tokenData.expiresIn(), account.getTenant().getSlug());
    }

    private Tenant findTenant(String tenantSlug) {
        return tenantRepository.findBySlugIgnoreCase(tenantSlug)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + tenantSlug));
    }
}
