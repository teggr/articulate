package com.teggr.articulate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        requestCache.setMatchingRequestParameterName(null);

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/").permitAll()
                .requestMatchers(HttpMethod.GET, "/app.css", "/favicon.ico", "/**/*.css", "/**/*.js").permitAll()
                        .requestMatchers("/generate/**").authenticated()
                .requestMatchers("/articles/**").authenticated()
                        .anyRequest().authenticated())
                .requestCache(cache -> cache.requestCache(requestCache))
                .formLogin(form -> form.defaultSuccessUrl("/articles"))
                .logout(logout -> logout.logoutSuccessUrl("/"));

        return http.build();
    }
}
