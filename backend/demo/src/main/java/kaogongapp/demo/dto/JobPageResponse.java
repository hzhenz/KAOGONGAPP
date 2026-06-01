package kaogongapp.demo.dto;

import java.util.List;

public record JobPageResponse(
        List<JobItem> items,
        long total,
        int page,
        int size,
        int totalPages
) {
}
