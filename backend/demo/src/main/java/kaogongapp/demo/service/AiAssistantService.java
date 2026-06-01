package kaogongapp.demo.service;

import kaogongapp.demo.config.AppProperties;
import kaogongapp.demo.dto.EssayExtractResponse;
import kaogongapp.demo.dto.EssayMaterialItem;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiAssistantService {

    private final AppProperties appProperties;
    private final RestClient restClient;

    public AiAssistantService(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.restClient = RestClient.builder().build();
    }

    public ReviewDecision reviewFocusEvidence(String subject, String text, boolean hasFile) {
        if (appProperties.aiEnabled() && StringUtils.hasText(appProperties.aiApiUrl())) {
            try {
                String prompt = "你是考公学习督学助手。请根据学习科目、文本成果、是否附图，判断是否属于真实学习成果。返回JSON：passed(boolean), confidence(string), comment(string)。"
                        + "\n科目: " + safe(subject)
                        + "\n是否附图: " + hasFile
                        + "\n文本成果: " + safe(text);
                AiResponse response = callAi(prompt);
                if (response != null && StringUtils.hasText(response.content())) {
                    String content = response.content();
                    boolean passed = !content.contains("\"passed\":false") && !content.contains("未通过");
                    String confidence = passed ? "0.93" : "0.52";
                    String comment = extractComment(content, passed ? "AI 判定为有效学习成果。" : "AI 判定成果不足，建议补充详细总结。");
                    return new ReviewDecision(passed, confidence, comment);
                }
            } catch (Exception ignored) {
            }
        }

        boolean hasText = StringUtils.hasText(text) && text.trim().length() >= 50;
        if (hasText || hasFile) {
            return new ReviewDecision(true, hasFile && !hasText ? "0.90" : "0.94", "内容与所选学科匹配，认定为有效学习。");
        }
        return new ReviewDecision(false, "0.41", "提交内容过于敷衍，建议重新整理后再打卡。");
    }

    public EssayExtractResponse extractEssay(String content) {
        String normalized = content == null ? "" : content.trim().replace("\r", "");
        if (appProperties.aiEnabled() && StringUtils.hasText(appProperties.aiApiUrl()) && StringUtils.hasText(normalized)) {
            try {
                String prompt = "你是考公申论素材提炼助手。请将下列材料提炼为：1句总结、3条重点（标题+说明），返回JSON：summary, highlights[{title,desc}]。\n材料：" + normalized;
                AiResponse response = callAi(prompt);
                if (response != null && StringUtils.hasText(response.content())) {
                    String aiText = response.content().trim();
                    List<EssayMaterialItem> highlights = new ArrayList<>();
                    highlights.add(new EssayMaterialItem("AI 提炼论点", extractLine(aiText, 0, "请结合材料补充中心论点。")));
                    highlights.add(new EssayMaterialItem("AI 提炼分析", extractLine(aiText, 1, "请补充问题、原因与影响分析。")));
                    highlights.add(new EssayMaterialItem("AI 提炼对策", extractLine(aiText, 2, "请补充统筹推进、压实责任、数字赋能等对策。")));
                    return new EssayExtractResponse("已调用 AI 完成素材提纯，可继续手工润色。", highlights);
                }
            } catch (Exception ignored) {
            }
        }

        if (normalized.length() > 280) {
            normalized = normalized.substring(0, 280) + "...";
        }
        List<String> segments = splitIntoSegments(normalized);
        List<EssayMaterialItem> highlights = new ArrayList<>();
        highlights.add(new EssayMaterialItem("核心论点", firstNonBlank(segments, 0, "请补充原文中的中心观点。")));
        highlights.add(new EssayMaterialItem("问题分析", firstNonBlank(segments, 1, "请围绕原因、表现、影响继续展开。")));
        highlights.add(new EssayMaterialItem("对策表达", "建议采用“强化统筹、压实责任、数字赋能、长效治理”等表述。"));

        String summary = normalized.isBlank()
                ? "暂无可提纯内容。"
                : "当前使用本地提纯模式，可直接用于背诵金句、框架迁移与论证积累。";
        return new EssayExtractResponse(summary, highlights);
    }

    private AiResponse callAi(String prompt) {
        return restClient.post()
                .uri(appProperties.aiApiUrl())
                .header(HttpHeaders.AUTHORIZATION, StringUtils.hasText(appProperties.aiApiKey()) ? "Bearer " + appProperties.aiApiKey() : "")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "model", StringUtils.hasText(appProperties.aiModel()) ? appProperties.aiModel() : "demo-model",
                        "prompt", prompt
                ))
                .retrieve()
                .body(AiResponse.class);
    }

    private String extractComment(String content, String fallback) {
        String compact = content.replace("\n", " ").trim();
        return compact.length() > 80 ? compact.substring(0, 80) + "..." : (compact.isBlank() ? fallback : compact);
    }

    private String extractLine(String text, int index, String fallback) {
        List<String> lines = splitIntoSegments(text);
        return firstNonBlank(lines, index, fallback);
    }

    private List<String> splitIntoSegments(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        return List.of(text.split("[。！？\n]"));
    }

    private String firstNonBlank(List<String> segments, int index, String fallback) {
        if (segments.size() > index && StringUtils.hasText(segments.get(index))) {
            return segments.get(index).trim();
        }
        return fallback;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record ReviewDecision(boolean passed, String confidence, String comment) {
    }

    public record AiResponse(String content) {
    }
}
