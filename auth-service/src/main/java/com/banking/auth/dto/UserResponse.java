package com.banking.auth.dto;

import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        Set<String> roles
) {
}
