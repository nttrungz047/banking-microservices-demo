package com.banking.auth.service;

import com.banking.auth.dto.AuthResponse;
import com.banking.auth.dto.LoginRequest;
import com.banking.auth.dto.RefreshRequest;
import com.banking.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshRequest request);
}
