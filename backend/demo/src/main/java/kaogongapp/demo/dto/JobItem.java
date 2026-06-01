package kaogongapp.demo.dto;

public record JobItem(
        Long id,
        String name,
        String ratio,
        String score,
        String tag,
        String province,
        String major,
        String year
) {
}
