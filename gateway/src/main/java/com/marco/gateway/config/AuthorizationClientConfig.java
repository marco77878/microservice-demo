package com.marco.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class AuthorizationClientConfig {

    @Bean
    public SecurityWebFilterChain authorizationClientSecurityFilterChain(ServerHttpSecurity http) {

        //uri放行
        String[] ignoreUrls = new String[]{"/oauth2/**", "/*.html", "/favicon.ico", "/webjars/**"};
        //禁用csrf与cors
        // 禁用csrf与cors
        http.csrf(ServerHttpSecurity.CsrfSpec::disable);
        http.cors(ServerHttpSecurity.CorsSpec::disable);
        //客户端设置
        http
                .authorizeExchange(authorize ->
                        authorize.pathMatchers(ignoreUrls).permitAll()
                                //其他请求，需要认证
                                .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())

                )
                .oauth2Login(Customizer.withDefaults())
                .oauth2Client(withDefaults());
        return http.build();
    }


}