package kaogongapp.demo.dto;

public record FocusSubmitResponse(
        String sessionId,
        String status,
        String confidence,
        String comment,
        boolean passed,
        String reviewedAt,
        String submitDeadline,
        int minTextLength,
        long maxFileSize
) {
}
