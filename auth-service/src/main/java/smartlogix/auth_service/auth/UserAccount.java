package smartlogix.auth_service.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "user_accounts",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_tenant_username", columnNames = {"tenant_id", "username"})
)
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 120)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 80)
    private String role;

    protected UserAccount() {
    }

    public UserAccount(Tenant tenant, String username, String passwordHash, String role) {
        this.tenant = tenant;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }
}
