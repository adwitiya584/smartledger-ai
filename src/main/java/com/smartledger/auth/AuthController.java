package com.smartledger.auth;

import com.smartledger.config.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          OtpService otpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpService = otpService;
    }

    // Step 1 — Send OTP
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Email already registered"));
        }

        try {
            otpService.sendOtp(email);
            return ResponseEntity.ok(Map.of("message", "OTP sent to " + email));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("message", "Failed to send OTP. Check email configuration."));
        }
    }

    // Step 2 — Verify OTP + Register
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");
        String name = body.get("name");
        String password = body.get("password");

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Email already registered"));
        }

        if (!otpService.verifyOtp(email, otp)) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Invalid or expired OTP"));
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmailVerified(true);
        userRepository.save(user);
        user.setTrialStartDate(LocalDateTime.now());
        user.setSubscriptionPlan("FREE_TRIAL");
        userRepository.save(user);

        String token = jwtService.generateToken(email);
        return ResponseEntity.ok(Map.of(
            "token", token,
            "name", user.getName(),
            "email", user.getEmail()
        ));
    }

    // Login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        return userRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(password, u.getPassword()))
                .map(u -> {
                    String token = jwtService.generateToken(email);
                    return ResponseEntity.ok(Map.of(
                        "token", token,
                        "name", u.getName(),
                        "email", u.getEmail()
                    ));
                })
                .orElse(ResponseEntity.status(401)
                    .body(Map.of("message", "Invalid email or password")));
    }

    // Get current user profile
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .map(u -> ResponseEntity.ok(Map.of(
                    "name", u.getName(),
                    "email", u.getEmail(),
                    "emailVerified", u.isEmailVerified()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    // Delete account
    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteAccount(
            Authentication auth,
            @RequestBody Map<String, String> body) {
        String password = body.get("password");

        return userRepository.findByEmail(auth.getName())
                .filter(u -> passwordEncoder.matches(password, u.getPassword()))
                .map(u -> {
                    userRepository.delete(u);
                    return ResponseEntity.ok(
                        Map.of("message", "Account deleted successfully"));
                })
                .orElse(ResponseEntity.status(401)
                    .body(Map.of("message", "Incorrect password")));
    }
}