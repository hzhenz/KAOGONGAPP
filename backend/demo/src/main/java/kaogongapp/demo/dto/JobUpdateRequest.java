package kaogongapp.demo.dto;

public record JobUpdateRequest(
        String name,
        String ratio,
        String score,
        String tag,
        String province,
        String major,
        String year
) {
}
