package kaogongapp.demo.controller;

import kaogongapp.demo.dto.DashboardResponse;
import kaogongapp.demo.dto.FocusHistoryResponse;
import kaogongapp.demo.dto.FocusStartRequest;
import kaogongapp.demo.dto.FocusStartResponse;
import kaogongapp.demo.dto.FocusSubmitResponse;
import kaogongapp.demo.dto.JobPageResponse;
import kaogongapp.demo.security.AppUserPrincipal;
import kaogongapp.demo.service.AppDataService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DemoController {

    private final AppDataService appDataService;

    public DemoController(AppDataService appDataService) {
        this.appDataService = appDataService;
    }

    @GetMapping("/overview")
    public DashboardResponse overview(Authentication authentication) {
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        return appDataService.overview(principal.username());
    }

    @GetMapping("/focus/history")
    public FocusHistoryResponse focusHistory(Authentication authentication) {
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        return appDataService.focusHistory(principal.username());
    }

    @PostMapping(value = "/focus/start", consumes = MediaType.APPLICATION_JSON_VALUE)
    public FocusStartResponse start(
            Authentication authentication,
            @Valid @RequestBody FocusStartRequest request
    ) {
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        return appDataService.startFocus(principal.username(), request);
    }

    @PostMapping(value = "/focus/submit", consumes = MediaType.APPLICATION_JSON_VALUE)
    public FocusSubmitResponse submit(
            Authentication authentication,
            @RequestBody Map<String, String> payload
    ) {
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        return appDataService.submitEvidence(
                principal.username(),
                payload.getOrDefault("sessionId", ""),
                payload.getOrDefault("subject", ""),
                payload.getOrDefault("mode", "学习小结"),
                payload.getOrDefault("summary", "")
        );
    }

    @GetMapping("/jobs")
    public JobPageResponse jobs(
            @RequestParam(required = false, defaultValue = "2024") String year,
            @RequestParam(required = false, defaultValue = "广东") String province,
            @RequestParam(required = false, defaultValue = "法学") String major,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "6") int size
    ) {
        return appDataService.findJobs(year, province, major, page, size);
    }
}
