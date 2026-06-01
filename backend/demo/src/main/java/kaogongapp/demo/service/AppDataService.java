package kaogongapp.demo.service;

import kaogongapp.demo.dto.AdminDashboardResponse;
import kaogongapp.demo.dto.DashboardResponse;
import kaogongapp.demo.dto.EssayExtractResponse;
import kaogongapp.demo.dto.EssayMaterialItem;
import kaogongapp.demo.dto.FocusHistoryItem;
import kaogongapp.demo.dto.FocusHistoryResponse;
import kaogongapp.demo.dto.FocusRecordItem;
import kaogongapp.demo.dto.FocusStartRequest;
import kaogongapp.demo.dto.FocusStartResponse;
import kaogongapp.demo.dto.FocusSubmitResponse;
import kaogongapp.demo.dto.JobCreateRequest;
import kaogongapp.demo.dto.JobItem;
import kaogongapp.demo.dto.JobPageResponse;
import kaogongapp.demo.dto.JobUpdateRequest;
import kaogongapp.demo.dto.UserItem;
import kaogongapp.demo.entity.AppUserEntity;
import kaogongapp.demo.entity.FocusRecordEntity;
import kaogongapp.demo.entity.FocusSessionEntity;
import kaogongapp.demo.entity.JobEntity;
import kaogongapp.demo.repository.AppUserRepository;
import kaogongapp.demo.repository.FocusRecordRepository;
import kaogongapp.demo.repository.FocusSessionRepository;
import kaogongapp.demo.repository.JobRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AppDataService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int REQUIRED_TEXT_LENGTH = 50;
    private static final int SUMMARY_PREVIEW_LENGTH = 60;
    private static final long SUBMIT_WINDOW_MINUTES = 5;
    private static final int MIN_FOCUS_SECONDS = 5;
    private static final int MAX_FOCUS_SECONDS = 180 * 60;

    private final AppUserRepository appUserRepository;
    private final FocusSessionRepository focusSessionRepository;
    private final FocusRecordRepository focusRecordRepository;
    private final JobRepository jobRepository;
    private final FileStorageService fileStorageService;
    private final AiAssistantService aiAssistantService;

    private final List<EssayMaterialItem> essayMaterials = List.of(
            new EssayMaterialItem("基层治理金句", "聚焦“精细化治理、协同共治”表达。"),
            new EssayMaterialItem("申论框架", "开头-分析-对策-升华的标准结构。"),
            new EssayMaterialItem("政策热词", "提炼高频政策表述与关键词。")
    );

    public AppDataService(
            AppUserRepository appUserRepository,
            FocusSessionRepository focusSessionRepository,
            FocusRecordRepository focusRecordRepository,
            JobRepository jobRepository,
            FileStorageService fileStorageService,
            AiAssistantService aiAssistantService
    ) {
        this.appUserRepository = appUserRepository;
        this.focusSessionRepository = focusSessionRepository;
        this.focusRecordRepository = focusRecordRepository;
        this.jobRepository = jobRepository;
        this.fileStorageService = fileStorageService;
        this.aiAssistantService = aiAssistantService;
    }

    public DashboardResponse overview(String username) {
        List<FocusRecordEntity> allRecords = focusRecordRepository.findByUsernameOrderByReviewedAtDesc(username);

        LocalDate today = LocalDate.now();
        int todayFocusMinutes = allRecords.stream()
                .filter(FocusRecordEntity::isPassed)
                .filter(item -> item.getReviewedAt().toLocalDate().equals(today))
                .mapToInt(FocusRecordEntity::getMinutes)
                .sum();

        long passedCount = allRecords.stream().filter(FocusRecordEntity::isPassed).count();
        int passRate = allRecords.isEmpty() ? 0 : (int) Math.round((passedCount * 100.0) / allRecords.size());

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("todayFocusMinutes", todayFocusMinutes);
        stats.put("completedSessions", passedCount);
        stats.put("aiPassRate", allRecords.isEmpty() ? "-" : passRate + "%");
        stats.put("jobsCount", jobRepository.count());

        LocalDate startDay = today.minusDays(13);
        List<FocusRecordEntity> recent = focusRecordRepository.findByUsernameAndReviewedAtAfterOrderByReviewedAtAsc(
                username,
                startDay.atStartOfDay().minusSeconds(1)
        );

        Map<LocalDate, Integer> dayCount = new HashMap<>();
        for (FocusRecordEntity item : recent) {
            if (item.isPassed()) {
                LocalDate key = item.getReviewedAt().toLocalDate();
                dayCount.put(key, dayCount.getOrDefault(key, 0) + 1);
            }
        }

        List<Integer> heatmapDays = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            LocalDate day = startDay.plusDays(i);
            heatmapDays.add(Math.min(5, dayCount.getOrDefault(day, 0)));
        }

        List<JobItem> jobItems = jobRepository.findAll().stream()
                .sorted(Comparator.comparing(JobEntity::getYear).reversed())
                .map(this::toJobItem)
                .toList();

        List<UserItem> users = appUserRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toUserItem)
                .toList();

        List<FocusRecordItem> recentRecords = allRecords.stream()
                .limit(6)
                .map(this::toFocusRecordItem)
                .toList();

        return new DashboardResponse(stats, heatmapDays, essayMaterials, jobItems, users, recentRecords);
    }

    public FocusHistoryResponse focusHistory(String username) {
        List<FocusHistoryItem> records = focusRecordRepository.findByUsernameOrderByReviewedAtDesc(username).stream()
                .map(this::toFocusHistoryItem)
                .toList();
        return new FocusHistoryResponse(records);
    }

    public FocusStartResponse startFocus(String username, FocusStartRequest request) {
        List<FocusSessionEntity> activeSessions = focusSessionRepository.findByUsernameAndActiveTrue(username);
        for (FocusSessionEntity activeSession : activeSessions) {
            activeSession.setActive(false);
        }
        if (!activeSessions.isEmpty()) {
            focusSessionRepository.saveAll(activeSessions);
        }

        String sessionId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        int totalSeconds = normalizeFocusSeconds(request.totalSeconds());
        int minutes = Math.max(1, (int) Math.ceil(totalSeconds / 60.0));

        FocusSessionEntity session = new FocusSessionEntity();
        session.setSessionId(sessionId);
        session.setUsername(username);
        session.setSubject(request.subject());
        session.setMinutes(minutes);
        session.setMode(request.mode());
        session.setTotalSeconds(totalSeconds);
        session.setStartedAt(LocalDateTime.now());
        session.setActive(true);
        focusSessionRepository.save(session);

        return new FocusStartResponse(
                sessionId,
                request.subject(),
                minutes,
                totalSeconds,
                "倒计时中",
                "已创建专注会话，倒计时开始。",
                formatNow(session.getStartedAt())
        );
    }

    public FocusSubmitResponse submitEvidence(String username, String sessionId, String subject, String mode, String summary) {
        FocusSessionEntity session = focusSessionRepository.findBySessionIdAndUsernameAndActiveTrue(sessionId, username)
                .orElse(null);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在，请重新开始专注。");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timerEnd = session.getStartedAt().plusSeconds(session.getTotalSeconds());
        LocalDateTime submitDeadline = timerEnd.plusMinutes(SUBMIT_WINDOW_MINUTES);
        if (now.isBefore(timerEnd)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "倒计时尚未结束，暂不能提交验收。");
        }
        if (now.isAfter(submitDeadline)) {
            session.setActive(false);
            focusSessionRepository.save(session);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "提交超时，本次专注已作废，请重新开始。");
        }

        if (!StringUtils.hasText(subject)) {
            subject = session.getSubject();
        }
        if (!StringUtils.hasText(mode)) {
            mode = session.getMode();
        }

        String normalizedSummary = StringUtils.hasText(summary) ? summary.trim() : "";
        boolean hasText = normalizedSummary.length() >= REQUIRED_TEXT_LENGTH;

        String status;
        String confidence;
        String comment;
        boolean passed;
        if (!hasText) {
            status = "验收失败";
            confidence = "0.41";
            comment = "学习小结不足 50 字，请补充后重新提交。";
            passed = false;
        } else {
            AiAssistantService.ReviewDecision decision = aiAssistantService.reviewFocusEvidence(subject, normalizedSummary, false);
            passed = decision.passed();
            confidence = decision.confidence();
            comment = decision.comment();
            status = passed ? "验收通过" : "验收失败";
        }

        FocusRecordEntity record = new FocusRecordEntity();
        record.setUsername(username);
        record.setSubject(subject);
        record.setMode(mode);
        record.setMinutes(session.getMinutes());
        record.setHasText(hasText);
        record.setHasFile(false);
        record.setSummary(normalizedSummary);
        record.setPassed(passed);
        record.setConfidence(confidence);
        record.setComment(comment);
        record.setReviewedAt(now);
        focusRecordRepository.save(record);

        session.setActive(false);
        focusSessionRepository.save(session);

        return new FocusSubmitResponse(
                sessionId,
                status,
                confidence,
                comment,
                passed,
                formatNow(record.getReviewedAt()),
                formatNow(submitDeadline),
                REQUIRED_TEXT_LENGTH,
                0
        );
    }

    public JobPageResponse findJobs(String year, String province, String major, int page, int size) {
        List<JobItem> result = new ArrayList<>();
        List<JobEntity> jobs = jobRepository.findAll();
        for (JobEntity item : jobs) {
            boolean yearMatch = !StringUtils.hasText(year) || containsIgnoreCase(item.getYear(), year);
            boolean provinceMatch = !StringUtils.hasText(province) || containsIgnoreCase(item.getProvince(), province);
            boolean majorMatch = !StringUtils.hasText(major) || containsIgnoreCase(item.getMajor(), major);
            if (yearMatch && provinceMatch && majorMatch) {
                result.add(toJobItem(item));
            }
        }

        result.sort(Comparator.comparing(JobItem::year).reversed().thenComparing(JobItem::name));
        int safeSize = Math.max(1, size);
        int safePage = Math.max(1, page);
        int fromIndex = Math.min((safePage - 1) * safeSize, result.size());
        int toIndex = Math.min(fromIndex + safeSize, result.size());
        int totalPages = result.isEmpty() ? 0 : (int) Math.ceil(result.size() / (double) safeSize);
        return new JobPageResponse(result.subList(fromIndex, toIndex), result.size(), safePage, safeSize, totalPages);
    }

    public EssayExtractResponse extractEssay(String content) {
        return aiAssistantService.extractEssay(content);
    }

    public AdminDashboardResponse adminOverview() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long totalUsers = appUserRepository.count();
        long activeUsers = appUserRepository.findAll().stream().filter(user -> "active".equalsIgnoreCase(user.getStatus())).count();
        long totalJobs = jobRepository.count();
        long totalRecords = focusRecordRepository.count();
        long passedRecords = focusRecordRepository.findTop10ByOrderByReviewedAtDesc().stream().filter(FocusRecordEntity::isPassed).count();

        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("totalJobs", totalJobs);
        stats.put("recentPassCount", passedRecords);
        stats.put("totalFocusRecords", totalRecords);

        List<UserItem> users = appUserRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toUserItem)
                .toList();
        List<JobItem> jobs = jobRepository.findAll().stream()
                .sorted(Comparator.comparing(JobEntity::getYear).reversed())
                .map(this::toJobItem)
                .toList();
        List<FocusRecordItem> recentRecords = focusRecordRepository.findTop10ByOrderByReviewedAtDesc().stream()
                .map(this::toFocusRecordItem)
                .toList();

        return new AdminDashboardResponse(stats, users, jobs, recentRecords);
    }

    public UserItem updateUserStatus(String username, String status) {
        String normalizedStatus = status == null ? "" : status.trim().toLowerCase();
        if (!List.of("active", "blocked").contains(normalizedStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "状态仅支持 active 或 blocked");
        }
        AppUserEntity user = appUserRepository.findByUsername(username).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在")
        );
        user.setStatus(normalizedStatus);
        appUserRepository.save(user);
        return toUserItem(user);
    }

    public JobItem createJob(JobCreateRequest request) {
        JobEntity entity = new JobEntity();
        entity.setName(request.name().trim());
        entity.setRatio(request.ratio().trim());
        entity.setScore(request.score().trim());
        entity.setTag(request.tag().trim());
        entity.setProvince(request.province().trim());
        entity.setMajor(request.major().trim());
        entity.setYear(request.year().trim());
        entity = jobRepository.save(entity);
        return toJobItem(entity);
    }

    public JobItem updateJob(Long id, JobUpdateRequest request) {
        JobEntity entity = jobRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "岗位不存在")
        );
        if (StringUtils.hasText(request.name())) entity.setName(request.name().trim());
        if (StringUtils.hasText(request.ratio())) entity.setRatio(request.ratio().trim());
        if (StringUtils.hasText(request.score())) entity.setScore(request.score().trim());
        if (StringUtils.hasText(request.tag())) entity.setTag(request.tag().trim());
        if (StringUtils.hasText(request.province())) entity.setProvince(request.province().trim());
        if (StringUtils.hasText(request.major())) entity.setMajor(request.major().trim());
        if (StringUtils.hasText(request.year())) entity.setYear(request.year().trim());
        entity = jobRepository.save(entity);
        return toJobItem(entity);
    }

    public void deleteJob(Long id) {
        if (!jobRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "岗位不存在");
        }
        jobRepository.deleteById(id);
    }

    private FocusRecordItem toFocusRecordItem(FocusRecordEntity item) {
        return new FocusRecordItem(
                item.getSubject(),
                item.getMode(),
                item.getMinutes(),
                item.isPassed(),
                item.getConfidence(),
                item.getComment(),
                previewSummary(item.getSummary()),
                formatNow(item.getReviewedAt())
        );
    }

    private FocusHistoryItem toFocusHistoryItem(FocusRecordEntity item) {
        return new FocusHistoryItem(
                item.getSubject(),
                item.getMinutes(),
                item.getMode(),
                item.isPassed(),
                item.getConfidence(),
                item.getComment(),
                item.getSummary(),
                formatNow(item.getReviewedAt())
        );
    }

    private UserItem toUserItem(AppUserEntity item) {
        return new UserItem(item.getUsername(), item.getDisplayName(), item.getRole(), item.getStatus());
    }

    private JobItem toJobItem(JobEntity item) {
        return new JobItem(
                item.getId(),
                item.getName(),
                item.getRatio(),
                item.getScore(),
                item.getTag(),
                item.getProvince(),
                item.getMajor(),
                item.getYear()
        );
    }

    private String previewSummary(String summary) {
        if (!StringUtils.hasText(summary)) {
            return "未填写学习小结";
        }
        String normalized = summary.trim();
        return normalized.length() > SUMMARY_PREVIEW_LENGTH
                ? normalized.substring(0, SUMMARY_PREVIEW_LENGTH) + "..."
                : normalized;
    }

    private int normalizeFocusSeconds(int totalSeconds) {
        return Math.max(MIN_FOCUS_SECONDS, Math.min(MAX_FOCUS_SECONDS, totalSeconds));
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        if (!StringUtils.hasText(source) || !StringUtils.hasText(keyword)) {
            return false;
        }
        return source.toLowerCase().contains(keyword.toLowerCase());
    }

    private String formatNow(LocalDateTime dateTime) {
        return dateTime.format(DATETIME_FORMATTER);
    }
}
