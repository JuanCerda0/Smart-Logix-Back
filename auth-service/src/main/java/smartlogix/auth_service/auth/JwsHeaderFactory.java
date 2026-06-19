package smartlogix.auth_service.auth;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;

final class JwsHeaderFactory {

    private JwsHeaderFactory() {
    }

    static JwsHeader hs256() {
        return JwsHeader.with(MacAlgorithm.HS256).build();
    }
}
