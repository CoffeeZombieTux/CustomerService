package io.customerservice.customerservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppPropertiesConfig {

    @Bean
    public AppProperties.Jwt jwtProperties(AppProperties appProperties) {
        return appProperties.jwt();
    }

    @Bean
    public AppProperties.Security securityProperties(AppProperties appProperties) {
        return appProperties.security();
    }

    @Bean
    public AppProperties.Agreements agreementsProperties(AppProperties appProperties) {
        return appProperties.agreements();
    }

    @Bean
    public AppProperties.Cleanup cleanupProperties(AppProperties appProperties) {
        return appProperties.cleanup();
    }
}
