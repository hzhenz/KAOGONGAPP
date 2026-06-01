package kaogongapp.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String name,
        String version,
        String jwtSecret,
        long jwtExpireMinutes,
        String uploadDir,
        boolean zeroRetention,
        boolean aiEnabled,
        String aiApiUrl,
        String aiApiKey,
        String aiModel
) {
}
