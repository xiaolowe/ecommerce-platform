package com.example.commerce.auth.config;

import com.example.commerce.auth.auth.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private final AuthService authService;
    private final String email;
    private final String password;
    private final String displayName;

    public AdminBootstrap(AuthService authService,
                          @Value("${bootstrap.admin.email:}") String email,
                          @Value("${bootstrap.admin.password:}") String password,
                          @Value("${bootstrap.admin.display-name:Platform Admin}") String displayName) {
        this.authService = authService;
        this.email = email;
        this.password = password;
        this.displayName = displayName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (StringUtils.hasText(email) && StringUtils.hasText(password)) {
            authService.ensureAdmin(email, password, displayName);
        }
    }
}
