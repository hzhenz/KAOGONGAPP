package kaogongapp.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record JobCreateRequest(
        @NotBlank String name,
        @NotBlank String ratio,
        @NotBlank String score,
        @NotBlank String tag,
        @NotBlank String province,
        @NotBlank String major,
        @NotBlank String year
) {
}
