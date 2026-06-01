package kaogongapp.demo.dto;

public record FocusHistoryItem(
        String subject,
        int minutes,
        String mode,
        boolean passed,
        String confidence,
        String comment,
        String summary,
        String reviewedAt
) {
}
