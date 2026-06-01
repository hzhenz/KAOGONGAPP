package kaogongapp.demo.controller;

import jakarta.validation.Valid;
import kaogongapp.demo.dto.AuthResponse;
import kaogongapp.demo.dto.LoginRequest;
import kaogongapp.demo.dto.MeResponse;
import kaogongapp.demo.dto.RegisterRequest;
import kaogongapp.demo.security.AppUserPrincipal;
import kaogongapp.demo.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        return authService.me(principal.username());
    }
}
