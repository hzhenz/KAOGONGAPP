package kaogongapp.demo.repository;

import kaogongapp.demo.entity.FocusRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FocusRecordRepository extends JpaRepository<FocusRecordEntity, Long> {

    List<FocusRecordEntity> findByUsernameAndReviewedAtAfterOrderByReviewedAtAsc(String username, LocalDateTime reviewedAt);

    List<FocusRecordEntity> findByUsernameOrderByReviewedAtDesc(String username);

    List<FocusRecordEntity> findTop10ByOrderByReviewedAtDesc();
}

