package kaogongapp.demo.controller;

import jakarta.validation.Valid;
import kaogongapp.demo.dto.EssayExtractRequest;
import kaogongapp.demo.dto.EssayExtractResponse;
import kaogongapp.demo.service.AppDataService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/essay")
public class EssayController {

    private final AppDataService appDataService;

    public EssayController(AppDataService appDataService) {
        this.appDataService = appDataService;
    }

    @PostMapping("/extract")
    public EssayExtractResponse extract(@Valid @RequestBody EssayExtractRequest request) {
        return appDataService.extractEssay(request.content());
    }
}
