package kaogongapp.demo.service;

import kaogongapp.demo.dto.AuthResponse;
import kaogongapp.demo.dto.LoginRequest;
import kaogongapp.demo.dto.MeResponse;
import kaogongapp.demo.dto.RegisterRequest;
import kaogongapp.demo.entity.AppUserEntity;
import kaogongapp.demo.repository.AppUserRepository;
import kaogongapp.demo.security.AppUserPrincipal;
import kaogongapp.demo.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim().toLowerCase();
        if (appUserRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        }

        AppUserEntity user = new AppUserEntity();
        user.setUsername(username);
        user.setDisplayName(request.displayName().trim());
        user.setRole("student");
        user.setStatus("active");
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user = appUserRepository.save(user);
        return toAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        String username = request.username().trim().toLowerCase();
        AppUserEntity user = appUserRepository.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!"active".equalsIgnoreCase(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号已被禁用，请联系管理员");
        }
        return toAuthResponse(user);
    }

    public MeResponse me(String username) {
        AppUserEntity user = appUserRepository.findByUsername(username).orElse(null);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录");
        }
        return new MeResponse(user.getUsername(), user.getDisplayName(), user.getRole());
    }

    public AppUserPrincipal loadUserPrincipal(String username) {
        AppUserEntity user = appUserRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return null;
        }
        return new AppUserPrincipal(user.getUsername(), user.getDisplayName(), user.getRole(), user.getPasswordHash());
    }

    public void requireAdmin(String username) {
        AppUserEntity user = appUserRepository.findByUsername(username).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录")
        );
        if (!"admin".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅管理员可执行该操作");
        }
    }

    private AuthResponse toAuthResponse(AppUserEntity user) {
        String token = jwtService.createToken(user.getUsername(), user.getDisplayName(), user.getRole());
        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpireSeconds(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole()
        );
    }
}
