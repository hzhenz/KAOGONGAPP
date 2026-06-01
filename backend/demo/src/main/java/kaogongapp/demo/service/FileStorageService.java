package kaogongapp.demo.service;

import kaogongapp.demo.config.AppProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path rootDir;
    private final boolean zeroRetention;

    public FileStorageService(AppProperties appProperties) {
        this.rootDir = Path.of(appProperties.uploadDir()).toAbsolutePath().normalize();
        this.zeroRetention = appProperties.zeroRetention();
    }

    public StoredFile store(String username, String sessionId, MultipartFile file) {
        try {
            Files.createDirectories(rootDir);
            String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename());
            String extension = getExtension(originalName);
            String safeName = username + "-" + sessionId + "-" + UUID.randomUUID().toString().replace("-", "") + extension;
            Path target = rootDir.resolve(safeName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(originalName, target.toString(), file.getContentType(), file.getSize());
        } catch (IOException exception) {
            throw new IllegalStateException("文件保存失败", exception);
        }
    }

    public void deleteQuietly(String storedPath) {
        if (!StringUtils.hasText(storedPath)) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(storedPath));
        } catch (IOException ignored) {
        }
    }

    public boolean isZeroRetention() {
        return zeroRetention;
    }

    private String getExtension(String originalName) {
        int index = originalName.lastIndexOf('.');
        if (index < 0 || index == originalName.length() - 1) {
            return "";
        }
        return originalName.substring(index);
    }

    public record StoredFile(String originalName, String storedPath, String contentType, long size) {
    }
}
