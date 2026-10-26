package org.elis.progettoesempio.config;

import org.elis.progettoesempio.repository.UtenteRepository;
import org.elis.progettoesempio.security.GestoreErroriSicurezza;
import org.elis.progettoesempio.security.JwtAuthFilter;
import org.elis.progettoesempio.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final UtenteRepository utenteRepository;
    private final GestoreErroriSicurezza gestoreErroriSicurezza;

    /** BCrypt: le password si salvano solo come hash con sale */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain catenaFiltri(HttpSecurity http) throws Exception {
        http
                // Nessuna sessione né cookie: il token viaggia nell'intestazione, quindi CSRF non serve
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessione -> sessione.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Usa le origini ammesse definite in WebConfig (app.cors.origini)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(richieste -> richieste
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/notizie").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(eccezioni -> eccezioni
                        .authenticationEntryPoint(gestoreErroriSicurezza)
                        .accessDeniedHandler(gestoreErroriSicurezza))
                .addFilterBefore(new JwtAuthFilter(jwtService, utenteRepository),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}