package com.example.product;

import com.example.common.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:productdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
public class ProductAuthTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("Unauthenticated request should be forbidden")
    public void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER role can view products (GET /products)")
    public void testUserCanViewProducts() throws Exception {
        String userToken = jwtUtil.generateToken("user_1", "ROLE_USER");

        mockMvc.perform(get("/products")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("USER role cannot add products (POST /products)")
    public void testUserCannotAddProduct() throws Exception {
        String userToken = jwtUtil.generateToken("user_1", "ROLE_USER");

        mockMvc.perform(post("/products")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Unauthorized Item\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EDITOR role can add products (POST /products)")
    public void testEditorCanAddProduct() throws Exception {
        String editorToken = jwtUtil.generateToken("editor_1", "ROLE_EDITOR");

        mockMvc.perform(post("/products")
                .header("Authorization", "Bearer " + editorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"MacBook Air M3\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("MacBook Air M3"));
    }

    @Test
    @DisplayName("EDITOR role inherits USER view permission (GET /products)")
    public void testEditorCanViewProducts() throws Exception {
        String editorToken = jwtUtil.generateToken("editor_1", "ROLE_EDITOR");

        mockMvc.perform(get("/products")
                .header("Authorization", "Bearer " + editorToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PRODUCT_ADMIN role has full access (GET, POST, PUT, DELETE)")
    public void testAdminHasFullPermissions() throws Exception {
        String adminToken = jwtUtil.generateToken("adm_1", "ROLE_PRODUCT_ADMIN");

        // 1. GET
        mockMvc.perform(get("/products")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 2. POST
        mockMvc.perform(post("/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Admin Test Product\"}"))
                .andExpect(status().isCreated());

        // 3. PUT
        mockMvc.perform(put("/products/1")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Updated by Admin\"}"))
                .andExpect(status().isOk());

        // 4. DELETE
        mockMvc.perform(delete("/products/1")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
