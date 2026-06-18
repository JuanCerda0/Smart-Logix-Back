package smartlogix.auth_service.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final long expirationSeconds;

    public JwtTokenService(
        @Value("${security.jwt.secret}") String jwtSecret,
        @Value("${security.jwt.expiration-seconds}") long expirationSeconds
    ) {
        SecretKeySpec secretKey = new SecretKeySpec(
            jwtSecret.getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
        );
        this.jwtEncoder = new NimbusJwtEncoder(
            new com.nimbusds.jose.jwk.source.ImmutableSecret<>(secretKey)
        );
        this.expirationSeconds = expirationSeconds;
    }

    public TokenData generateToken(UserAccount userAccount) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationSeconds);
        String tenant = userAccount.getTenant().getSlug();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("smartlogix-auth-service")
            .subject(userAccount.getUsername())
            .issuedAt(now)
            .expiresAt(expiresAt)
            .claim("tenant", tenant)
            .claim("role", userAccount.getRole())
            .claim("scope", "products:read products:write")
            .build();

        String token = jwtEncoder
            .encode(JwtEncoderParameters.from(JwsHeaderFactory.hs256(), claims))
            .getTokenValue();

        return new TokenData(token, expirationSeconds);
    }
}
