package kaogongapp.demo.dto;

import java.util.List;

public record EssayExtractResponse(
        String summary,
        List<EssayMaterialItem> highlights
) {
}
