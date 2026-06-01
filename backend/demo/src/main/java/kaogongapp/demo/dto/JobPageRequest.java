package kaogongapp.demo.dto;

public record JobPageRequest(
        String year,
        String province,
        String major,
        int page,
        int size
) {
}
