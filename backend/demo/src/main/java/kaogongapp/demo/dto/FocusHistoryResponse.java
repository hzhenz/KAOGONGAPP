package kaogongapp.demo.dto;

import java.util.List;

public record FocusHistoryResponse(
        List<FocusHistoryItem> records
) {
}
