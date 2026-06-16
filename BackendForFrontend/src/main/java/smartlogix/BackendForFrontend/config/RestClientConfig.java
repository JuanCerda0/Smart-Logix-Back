package smartlogix.BackendForFrontend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    RestClient inventoryRestClient(RestClient.Builder builder, @Value("${smartlogix.services.inventory-url}") String inventoryUrl) {
        return builder
                .baseUrl(inventoryUrl)
                .build();
    }

    @Bean
    RestClient authRestClient(RestClient.Builder builder, @Value("${smartlogix.services.auth-url}") String authUrl) {
        return builder
                .baseUrl(authUrl)
                .build();
    }
}
