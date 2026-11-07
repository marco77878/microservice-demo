package com.example.uaa.config;

import com.example.uaa.entity.User;
import com.example.uaa.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initUser("user_1", "user_1", "ROLE_USER");
        initUser("editor_1", "editor_1", "ROLE_EDITOR");
        initUser("adm_1", "adm_1", "ROLE_PRODUCT_ADMIN");
        log.info("Database user initialization completed.");
    }

    private void initUser(String username, String rawPassword, String role) {
        if (userRepository.findByUsername(username).isEmpty()) {
            User user = new User(username, passwordEncoder.encode(rawPassword), role);
            userRepository.save(user);
            log.info("Created user: {} with role: {}", username, role);
        }
    }
}
