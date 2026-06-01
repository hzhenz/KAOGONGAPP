package kaogongapp.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record FocusStartRequest(
        @NotBlank String subject,
        @Min(5) @Max(180 * 60) int totalSeconds,
        @NotBlank String mode
) {
}
