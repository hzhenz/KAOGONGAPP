package kaogongapp.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record EssayExtractRequest(
        String sourceUrl,
        @NotBlank String content
) {
}
