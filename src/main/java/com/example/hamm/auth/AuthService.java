package com.example.hamm.auth;

import com.example.hamm.exception.ValidationException;
import com.example.hamm.security.JwtProperties;
import com.example.hamm.security.JwtService;
import com.example.hamm.user.Role;
import com.example.hamm.user.User;
import com.example.hamm.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, JwtService jwtService, JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ValidationException("An account with this email already exists");
        }

        // New self-registrations are always CUSTOMER; elevated roles must be
        // granted separately to prevent privilege escalation via registration.
        User user = User.builder()
                .email(request.email())
                .fullName(request.fullName())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return AuthResponse.of(token, jwtProperties.expirationMs());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ValidationException("Invalid email or password"));

        String token = jwtService.generateToken(user);
        return AuthResponse.of(token, jwtProperties.expirationMs());
    }
}
