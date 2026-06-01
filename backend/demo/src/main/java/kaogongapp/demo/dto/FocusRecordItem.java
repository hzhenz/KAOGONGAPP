package kaogongapp.demo.dto;

public record FocusRecordItem(
        String subject,
        String mode,
        int minutes,
        boolean passed,
        String confidence,
        String comment,
        String summary,
        String reviewedAt
) {
}
