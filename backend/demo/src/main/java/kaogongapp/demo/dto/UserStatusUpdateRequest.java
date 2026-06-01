package kaogongapp.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record UserStatusUpdateRequest(
        @NotBlank String status
) {
}
