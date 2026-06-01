package kaogongapp.demo.config;

import kaogongapp.demo.entity.AppUserEntity;
import kaogongapp.demo.entity.JobEntity;
import kaogongapp.demo.repository.AppUserRepository;
import kaogongapp.demo.repository.JobRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner seedData(
            AppUserRepository appUserRepository,
            JobRepository jobRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (!appUserRepository.existsByUsername("student")) {
                AppUserEntity student = new AppUserEntity();
                student.setUsername("student");
                student.setDisplayName("考公学生");
                student.setRole("student");
                student.setStatus("active");
                student.setPasswordHash(passwordEncoder.encode("123456"));
                appUserRepository.save(student);
            }

            if (!appUserRepository.existsByUsername("admin")) {
                AppUserEntity admin = new AppUserEntity();
                admin.setUsername("admin");
                admin.setDisplayName("系统管理员");
                admin.setRole("admin");
                admin.setStatus("active");
                admin.setPasswordHash(passwordEncoder.encode("admin123"));
                appUserRepository.save(admin);
            }

            if (jobRepository.count() == 0) {
                jobRepository.saveAll(List.of(
                        buildJob("某省直机关", "1:38", "128.5", "热门", "广东", "法学", "2024"),
                        buildJob("某地市税务局", "1:24", "119.0", "稳定", "广东", "法学", "2024"),
                        buildJob("某区市场监管", "1:17", "112.5", "可冲", "广东", "法学", "2024"),
                        buildJob("某市公安局", "1:51", "126.0", "竞争大", "江苏", "法学", "2024"),
                        buildJob("某省财政厅", "1:29", "121.0", "推荐", "广东", "财政学", "2024")
                ));
            }
        };
    }

    private JobEntity buildJob(String name, String ratio, String score, String tag, String province, String major, String year) {
        JobEntity entity = new JobEntity();
        entity.setName(name);
        entity.setRatio(ratio);
        entity.setScore(score);
        entity.setTag(tag);
        entity.setProvince(province);
        entity.setMajor(major);
        entity.setYear(year);
        return entity;
    }
}
