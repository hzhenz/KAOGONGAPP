package kaogongapp.demo.dto;

import java.util.List;
import java.util.Map;

public record AdminDashboardResponse(
        Map<String, Object> stats,
        List<UserItem> users,
        List<JobItem> jobs,
        List<FocusRecordItem> recentRecords
) {
}
