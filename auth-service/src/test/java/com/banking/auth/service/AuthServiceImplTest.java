package com.banking.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.auth.dto.AuthResponse;
import com.banking.auth.dto.LoginRequest;
import com.banking.auth.dto.RegisterRequest;
import com.banking.auth.entity.Role;
import com.banking.auth.entity.User;
import com.banking.auth.exception.DuplicateUserException;
import com.banking.auth.exception.InvalidCredentialsException;
import com.banking.auth.repository.RoleRepository;
import com.banking.auth.repository.UserRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = Role.builder().id(1L).name("ROLE_USER").build();
    }

    @Test
    void register_success_hashesPasswordAndReturnsTokens() {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123");
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh");

        AuthResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.username()).isEqualTo("alice");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(captor.getValue().getRoles()).containsExactly(userRole);
    }

    @Test
    void register_duplicateUsername_throws() {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123");
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateUserException.class)
                .hasMessageContaining("Username already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_wrongPassword_throws() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("alice")
                .email("alice@example.com")
                .passwordHash("hashed")
                .roles(Set.of(userRole))
                .build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_success_returnsTokens() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("alice")
                .email("alice@example.com")
                .passwordHash("hashed")
                .roles(Set.of(userRole))
                .build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh");

        AuthResponse response = authService.login(new LoginRequest("alice", "password123"));

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.username()).isEqualTo("alice");
    }
}
