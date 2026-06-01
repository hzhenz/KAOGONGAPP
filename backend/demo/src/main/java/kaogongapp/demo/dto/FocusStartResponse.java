package kaogongapp.demo.dto;

public record FocusStartResponse(
        String sessionId,
        String subject,
        int minutes,
        int totalSeconds,
        String status,
        String message,
        String startedAt
) {
}
