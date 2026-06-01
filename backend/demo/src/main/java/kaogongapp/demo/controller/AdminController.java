package kaogongapp.demo.controller;

import jakarta.validation.Valid;
import kaogongapp.demo.dto.AdminDashboardResponse;
import kaogongapp.demo.dto.JobCreateRequest;
import kaogongapp.demo.dto.JobItem;
import kaogongapp.demo.dto.JobUpdateRequest;
import kaogongapp.demo.dto.UserItem;
import kaogongapp.demo.dto.UserStatusUpdateRequest;
import kaogongapp.demo.security.AppUserPrincipal;
import kaogongapp.demo.service.AppDataService;
import kaogongapp.demo.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AppDataService appDataService;
    private final AuthService authService;

    public AdminController(AppDataService appDataService, AuthService authService) {
        this.appDataService = appDataService;
        this.authService = authService;
    }

    @GetMapping("/overview")
    public AdminDashboardResponse overview(Authentication authentication) {
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        authService.requireAdmin(principal.username());
        return appDataService.adminOverview();
    }

    @PatchMapping("/users/{username}/status")
    public UserItem updateUserStatus(
            Authentication authentication,
            @PathVariable String username,
            @Valid @RequestBody UserStatusUpdateRequest request
    ) {
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        authService.requireAdmin(principal.username());
        return appDataService.updateUserStatus(username, request.status());
    }

    @PostMapping("/jobs")
    public JobItem createJob(
            Authentication authentication,
            @Valid @RequestBody JobCreateRequest request
    ) {
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        authService.requireAdmin(principal.username());
        return appDataService.createJob(request);
    }

    @PutMapping("/jobs/{id}")
    public JobItem updateJob(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody JobUpdateRequest request
    ) {
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        authService.requireAdmin(principal.username());
        return appDataService.updateJob(id, request);
    }

    @DeleteMapping("/jobs/{id}")
    public void deleteJob(
            Authentication authentication,
            @PathVariable Long id
    ) {
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        authService.requireAdmin(principal.username());
        appDataService.deleteJob(id);
    }
}
