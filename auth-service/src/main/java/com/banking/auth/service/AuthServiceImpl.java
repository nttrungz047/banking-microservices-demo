package com.banking.auth.service;

import com.banking.auth.dto.AuthResponse;
import com.banking.auth.dto.LoginRequest;
import com.banking.auth.dto.RefreshRequest;
import com.banking.auth.dto.RegisterRequest;
import com.banking.auth.entity.Role;
import com.banking.auth.entity.User;
import com.banking.auth.exception.DuplicateUserException;
import com.banking.auth.exception.InvalidCredentialsException;
import com.banking.auth.exception.InvalidTokenException;
import com.banking.auth.exception.RoleNotFoundException;
import com.banking.auth.exception.UserNotFoundException;
import com.banking.auth.repository.RoleRepository;
import com.banking.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateUserException("Username already taken: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateUserException("Email already taken: " + request.email());
        }

        Role userRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new RoleNotFoundException("Default role not found: " + DEFAULT_ROLE));

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .roles(Set.of(userRole))
                .build();

        User saved = userRepository.save(user);
        log.info("Registered user id={} username={}", saved.getId(), saved.getUsername());
        return issueTokens(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        log.info("User logged in id={} username={}", user.getId(), user.getUsername());
        return issueTokens(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtService.parseClaims(request.refreshToken());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        if (!jwtService.isRefreshToken(claims)) {
            throw new InvalidTokenException("Token is not a refresh token");
        }

        UUID userId = UUID.fromString(claims.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        log.info("Refreshed tokens for user id={}", user.getId());
        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                "Bearer",
                user.getId(),
                user.getUsername()
        );
    }
}
