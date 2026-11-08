package com.example.uaa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.ldap.repository.config.EnableLdapRepositories;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

/**
 * Spring LDAP configuration using LdapContextSource and LdapTemplate.
 * Only activated when ldap.enabled=true (the default).
 *
 * When ldap.enabled=false (e.g. unit tests, local mode without LDAP container),
 * this entire configuration class is skipped, and no LdapTemplate / LdapRepository
 * beans are created. LdapAuthService gracefully falls back to predefined credentials.
 */
@Configuration
@ConditionalOnProperty(name = "ldap.enabled", havingValue = "true", matchIfMissing = true)
@EnableLdapRepositories(basePackages = "com.example.uaa.ldap")
public class LdapConfig {

    private static final Logger log = LoggerFactory.getLogger(LdapConfig.class);

    @Value("${spring.ldap.urls:ldap://localhost:389}")
    private String ldapUrl;

    @Value("${spring.ldap.base:dc=example,dc=org}")
    private String baseDn;

    @Value("${spring.ldap.username:cn=admin,dc=example,dc=org}")
    private String adminDn;

    @Value("${spring.ldap.password:adminpassword}")
    private String adminPassword;

    @Bean
    public LdapContextSource contextSource() {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(ldapUrl);
        contextSource.setBase(baseDn);
        contextSource.setUserDn(adminDn);
        contextSource.setPassword(adminPassword);
        contextSource.setPooled(true);
        contextSource.setBaseEnvironmentProperties(
                java.util.Map.of("com.sun.jndi.ldap.connect.timeout", "3000",
                                 "com.sun.jndi.ldap.read.timeout", "3000")
        );
        log.info("Configuring Spring LDAP ContextSource: url={}, base={}", ldapUrl, baseDn);
        return contextSource;
    }

    @Bean
    public LdapTemplate ldapTemplate(LdapContextSource contextSource) {
        LdapTemplate ldapTemplate = new LdapTemplate(contextSource);
        ldapTemplate.setIgnorePartialResultException(true);
        ldapTemplate.setIgnoreNameNotFoundException(true);
        return ldapTemplate;
    }
}
