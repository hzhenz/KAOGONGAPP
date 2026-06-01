package kaogongapp.demo.dto;

import java.util.List;
import java.util.Map;

public record DashboardResponse(
        Map<String, Object> stats,
        List<Integer> heatmapDays,
        List<EssayMaterialItem> essayMaterials,
        List<JobItem> jobResults,
        List<UserItem> users,
        List<FocusRecordItem> recentRecords
) {
}
