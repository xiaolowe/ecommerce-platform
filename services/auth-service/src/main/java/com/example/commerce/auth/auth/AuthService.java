package com.example.commerce.auth.auth;

import com.example.commerce.auth.user.UserAccount;
import com.example.commerce.auth.user.UserAccountMapper;
import com.example.commerce.auth.user.UserRole;
import com.example.commerce.platform.common.error.BadRequestException;
import com.example.commerce.platform.common.error.ConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import static com.example.commerce.auth.auth.AuthDtos.LoginRequest;
import static com.example.commerce.auth.auth.AuthDtos.RegisterRequest;
import static com.example.commerce.auth.auth.AuthDtos.TokenResponse;
import static com.example.commerce.auth.auth.AuthDtos.UserResponse;

@Service
public class AuthService {

    private final UserAccountMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserAccountMapper userMapper, PasswordEncoder passwordEncoder,
                       TokenService tokenService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userMapper.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }

        UserAccount user = new UserAccount(
                UUID.randomUUID().toString(),
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                UserRole.CUSTOMER,
                Instant.now()
        );
        try {
            userMapper.insert(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Email is already registered");
        }
        return tokenResponse(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        UserAccount user = userMapper.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }
        return tokenResponse(user);
    }

    @Transactional
    public UserAccount ensureAdmin(String email, String rawPassword, String displayName) {
        return userMapper.findByEmail(normalizeEmail(email)).orElseGet(() -> {
            UserAccount admin = new UserAccount(
                    UUID.randomUUID().toString(),
                    normalizeEmail(email),
                    passwordEncoder.encode(rawPassword),
                    displayName,
                    UserRole.ADMIN,
                    Instant.now()
            );
            userMapper.insert(admin);
            return admin;
        });
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String userId) {
        return userMapper.findById(userId)
                .map(this::userResponse)
                .orElseThrow(() -> new BadRequestException("Authenticated user no longer exists"));
    }

    private TokenResponse tokenResponse(UserAccount user) {
        TokenService.IssuedToken token = tokenService.issue(user);
        return new TokenResponse(token.value(), "Bearer", token.expiresInSeconds(), userResponse(user));
    }

    private UserResponse userResponse(UserAccount user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole().name());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
