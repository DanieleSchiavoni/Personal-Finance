package org.elis.progettoesempio.config;

import java.util.List;

import org.elis.progettoesempio.security.UtenteCorrenteArgumentResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final UtenteCorrenteArgumentResolver utenteCorrenteArgumentResolver;

    @Value("${app.cors.origini:http://localhost:4200}")
    private String[] originiAmmesse;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(utenteCorrenteArgumentResolver);
    }

    /** Il frontend Angular (altra porta) può chiamare le API. Solo le origini indicate in app.cors.origini. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(originiAmmesse)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition"); // servirà per scaricare il PDF
    }
}