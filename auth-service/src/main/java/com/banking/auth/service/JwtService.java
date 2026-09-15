package com.banking.auth.service;

import com.banking.auth.entity.User;
import io.jsonwebtoken.Claims;
import java.util.UUID;

public interface JwtService {

    String generateAccessToken(User user);

    String generateRefreshToken(User user);

    Claims parseClaims(String token);

    UUID extractUserId(String token);

    boolean isAccessToken(Claims claims);

    boolean isRefreshToken(Claims claims);
}
