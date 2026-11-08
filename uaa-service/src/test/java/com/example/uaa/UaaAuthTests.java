package com.example.uaa;

import com.example.common.JwtUtil;
import com.example.uaa.service.LdapAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:uaadb;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "eureka.client.enabled=false",
    "ldap.enabled=false",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration,org.springframework.boot.autoconfigure.data.ldap.LdapRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
public class UaaAuthTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LdapAuthService ldapAuthService;

    @Test
    @DisplayName("Test JWT generation and claims extraction")
    public void testJwtGenerationAndValidation() {
        String token = jwtUtil.generateToken("test_user", "ROLE_EDITOR");
        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
        assertEquals("test_user", jwtUtil.extractUsername(token));
        assertEquals("ROLE_EDITOR", jwtUtil.extractRole(token));
    }

    @Test
    @DisplayName("Test LDAP predefined users authentication (fallback mode)")
    public void testLdapPredefinedUsers() {
        assertEquals("ROLE_USER", ldapAuthService.authenticate("ldap_user_1", "ldap_user_1"));
        assertEquals("ROLE_EDITOR", ldapAuthService.authenticate("ldap_editor_1", "ldap_editor_1"));
        assertEquals("ROLE_PRODUCT_ADMIN", ldapAuthService.authenticate("ldap_adm_1", "ldap_adm_1"));
        assertNull(ldapAuthService.authenticate("ldap_user_1", "wrong_password"));
    }

    @Test
    @DisplayName("Test token endpoint for database user_1")
    public void testTokenDbUser() throws Exception {
        mockMvc.perform(post("/uaa/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "password")
                .param("username", "user_1")
                .param("password", "user_1")
                .param("client_id", "my_client")
                .param("client_secret", "my_secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.username").value("user_1"));
    }

    @Test
    @DisplayName("Test token endpoint for LDAP editor user (predefined fallback)")
    public void testTokenLdapEditor() throws Exception {
        mockMvc.perform(post("/uaa/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "password")
                .param("username", "ldap_editor_1")
                .param("password", "ldap_editor_1")
                .param("client_id", "my_client")
                .param("client_secret", "my_secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.role").value("ROLE_EDITOR"))
                .andExpect(jsonPath("$.username").value("ldap_editor_1"));
    }

    @Test
    @DisplayName("Test invalid credentials returns 401")
    public void testTokenBadCredentials() throws Exception {
        mockMvc.perform(post("/uaa/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "password")
                .param("username", "user_1")
                .param("password", "bad_pwd"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
    }
}
