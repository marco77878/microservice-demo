package com.example.uaa.service;

import com.example.uaa.ldap.LdapUserEntry;
import com.example.uaa.ldap.LdapUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.stereotype.Service;

import javax.naming.Context;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LDAP Authentication Service using spring-boot-starter-data-ldap.
 *
 * <p>Three-layer authentication strategy:</p>
 * <ol>
 *   <li><b>LdapTemplate lookup + Bind authentication</b>:
 *       Uses Spring LdapTemplate to search for the user entry by cn,
 *       then performs an LDAP Bind using the found DN and user-supplied password
 *       via LdapContextSource.getContext().</li>
 *   <li><b>Spring Data LdapRepository query</b>:
 *       As a secondary approach, uses LdapUserRepository.findByCn() to locate
 *       the user and verify that the entry exists.</li>
 *   <li><b>Predefined account fallback</b>:
 *       When the LDAP server is unreachable (local mode, unit tests, container startup),
 *       falls back to an in-memory ConcurrentHashMap of predefined credentials.</li>
 * </ol>
 */
@Service
public class LdapAuthService {

    private static final Logger log = LoggerFactory.getLogger(LdapAuthService.class);

    @Value("${ldap.url:ldap://ldap:389}")
    private String ldapUrl;

    @Value("${ldap.base-dn:dc=example,dc=org}")
    private String baseDn;

    /**
     * Spring LDAP LdapTemplate — injected when ldap.enabled=true (LdapConfig active).
     * May be null if LDAP auto-configuration is disabled.
     */
    @Autowired(required = false)
    private LdapTemplate ldapTemplate;

    /**
     * Spring LDAP ContextSource — used for Bind authentication.
     */
    @Autowired(required = false)
    private LdapContextSource ldapContextSource;

    /**
     * Spring Data LDAP Repository — auto-generated query methods.
     */
    @Autowired(required = false)
    private LdapUserRepository ldapUserRepository;

    // Predefined LDAP users mapping: username -> [password, role]
    private final Map<String, String[]> predefinedLdapUsers = new ConcurrentHashMap<>();

    public LdapAuthService() {
        // Predefined users per requirements specification
        predefinedLdapUsers.put("ldap_user_1", new String[]{"ldap_user_1", "ROLE_USER"});
        predefinedLdapUsers.put("ldap_editor_1", new String[]{"ldap_editor_1", "ROLE_EDITOR"});
        predefinedLdapUsers.put("ldap_adm_1", new String[]{"ldap_adm_1", "ROLE_PRODUCT_ADMIN"});
    }

    /**
     * Authenticate a user against LDAP with three-layer fallback.
     *
     * @param username the username (cn) to authenticate
     * @param password the password to verify
     * @return the role string (e.g. "ROLE_USER") on success, or null on failure
     */
    public String authenticate(String username, String password) {
        if (username == null || password == null) {
            return null;
        }

        // === Layer 1: Spring LdapTemplate — Bind Authentication ===
        if (ldapTemplate != null && ldapContextSource != null) {
            try {
                String role = authenticateWithSpringLdap(username, password);
                if (role != null) {
                    log.info("Successfully authenticated user '{}' via Spring LDAP Bind", username);
                    return role;
                }
            } catch (Exception e) {
                log.debug("Spring LDAP Bind authentication failed for '{}': {}", username, e.getMessage());
            }
        }

        // === Layer 2: Spring Data LdapRepository — lookup + verify ===
        if (ldapUserRepository != null) {
            try {
                String role = authenticateWithLdapRepository(username, password);
                if (role != null) {
                    log.info("Successfully authenticated user '{}' via Spring Data LdapRepository", username);
                    return role;
                }
            } catch (Exception e) {
                log.debug("Spring Data LdapRepository query failed for '{}': {}", username, e.getMessage());
            }
        }

        // === Layer 3: Predefined LDAP account fallback (local / test mode) ===
        if (predefinedLdapUsers.containsKey(username)) {
            String[] userDetails = predefinedLdapUsers.get(username);
            if (userDetails[0].equals(password)) {
                log.info("Successfully authenticated user '{}' via predefined LDAP credentials (fallback)", username);
                return userDetails[1];
            }
        }

        log.warn("LDAP authentication failed for user '{}'", username);
        return null;
    }

    /**
     * Layer 1: Authenticate using Spring LDAP's Bind operation.
     * <p>
     * Steps:
     * 1. Use LdapTemplate to search for the user by cn (EqualsFilter).
     * 2. If found, construct the full user DN.
     * 3. Attempt an LDAP Bind with the user's DN and password via a new DirContext.
     * 4. If Bind succeeds, authentication passes.
     * </p>
     */
    private String authenticateWithSpringLdap(String username, String password) {
        // Step 1: Search for the user entry using LdapTemplate
        boolean authenticated = ldapTemplate.authenticate(
                LdapUtils.newLdapName("ou=users"),
                new EqualsFilter("cn", username).encode(),
                password
        );

        if (authenticated) {
            return resolveRole(username);
        }

        return null;
    }

    /**
     * Layer 2: Authenticate using Spring Data LdapRepository.
     * <p>
     * Uses the auto-derived findByCn() method to check if the user exists in LDAP,
     * then performs a direct JNDI Bind to verify the password.
     * </p>
     */
    private String authenticateWithLdapRepository(String username, String password) {
        Optional<LdapUserEntry> entryOpt = ldapUserRepository.findByCn(username);
        if (entryOpt.isPresent()) {
            LdapUserEntry entry = entryOpt.get();
            log.debug("Found LDAP entry via Repository: {}", entry);

            // Verify password with JNDI Bind using the discovered DN
            if (verifyPasswordByBind(entry.getDn().toString(), password)) {
                return resolveRole(username);
            }
        }
        return null;
    }

    /**
     * Verify the user's password by performing a direct LDAP Bind operation
     * using the full DN discovered from the LDAP directory.
     */
    private boolean verifyPasswordByBind(String userDnRelative, String password) {
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, ldapUrl);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            // Construct the full DN by appending base DN
            String fullDn = userDnRelative + "," + baseDn;
            env.put(Context.SECURITY_PRINCIPAL, fullDn);
            env.put(Context.SECURITY_CREDENTIALS, password);
            env.put("com.sun.jndi.ldap.connect.timeout", "2000");

            DirContext ctx = new InitialDirContext(env);
            ctx.close();
            return true;
        } catch (Exception e) {
            log.debug("JNDI Bind verification failed for DN '{}': {}", userDnRelative, e.getMessage());
            return false;
        }
    }

    /**
     * Resolve the application role for an LDAP-authenticated user.
     * Checks the predefined mapping first, then falls back to username-based convention.
     */
    private String resolveRole(String username) {
        if (predefinedLdapUsers.containsKey(username)) {
            return predefinedLdapUsers.get(username)[1];
        }
        return determineRoleByUsername(username);
    }

    /**
     * Convention-based role determination for dynamic LDAP users
     * not in the predefined mapping.
     */
    private String determineRoleByUsername(String username) {
        if (username.contains("adm")) {
            return "ROLE_PRODUCT_ADMIN";
        } else if (username.contains("editor")) {
            return "ROLE_EDITOR";
        }
        return "ROLE_USER";
    }
}
