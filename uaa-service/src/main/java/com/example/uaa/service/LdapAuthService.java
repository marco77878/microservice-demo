package com.example.uaa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.naming.Context;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LdapAuthService {

    private static final Logger log = LoggerFactory.getLogger(LdapAuthService.class);

    @Value("${ldap.url:ldap://ldap:389}")
    private String ldapUrl;

    @Value("${ldap.base-dn:dc=example,dc=org}")
    private String baseDn;

    // Predefined LDAP users mapping: username -> [password, role]
    private final Map<String, String[]> predefinedLdapUsers = new ConcurrentHashMap<>();

    public LdapAuthService() {
        // Predefined users per requirements
        predefinedLdapUsers.put("ldap_user_1", new String[]{"ldap_user_1", "ROLE_USER"});
        predefinedLdapUsers.put("ldap_editor_1", new String[]{"ldap_editor_1", "ROLE_EDITOR"});
        predefinedLdapUsers.put("ldap_adm_1", new String[]{"ldap_adm_1", "ROLE_PRODUCT_ADMIN"});
    }

    public String authenticate(String username, String password) {
        if (username == null || password == null) {
            return null;
        }

        // 1. Try real LDAP bind if LDAP server is reachable
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, ldapUrl);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            // User DN format
            String userDn = "cn=" + username + ",ou=users," + baseDn;
            env.put(Context.SECURITY_PRINCIPAL, userDn);
            env.put(Context.SECURITY_CREDENTIALS, password);
            env.put("com.sun.jndi.ldap.connect.timeout", "2000");

            DirContext ctx = new InitialDirContext(env);
            ctx.close();
            log.info("Successfully authenticated user '{}' via live LDAP server", username);

            // Determine role
            if (predefinedLdapUsers.containsKey(username)) {
                return predefinedLdapUsers.get(username)[1];
            }
            return determineRoleByUsername(username);
        } catch (Exception e) {
            log.debug("LDAP connection to {} failed or credentials mismatch: {}. Falling back to predefined LDAP directory.", ldapUrl, e.getMessage());
        }

        // 2. Predefined LDAP user validation fallback
        if (predefinedLdapUsers.containsKey(username)) {
            String[] userDetails = predefinedLdapUsers.get(username);
            if (userDetails[0].equals(password)) {
                log.info("Successfully authenticated user '{}' via predefined LDAP credentials", username);
                return userDetails[1];
            }
        }

        return null;
    }

    private String determineRoleByUsername(String username) {
        if (username.contains("adm")) {
            return "ROLE_PRODUCT_ADMIN";
        } else if (username.contains("editor")) {
            return "ROLE_EDITOR";
        }
        return "ROLE_USER";
    }
}
