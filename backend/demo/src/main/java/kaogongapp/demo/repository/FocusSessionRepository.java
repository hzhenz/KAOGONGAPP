package kaogongapp.demo.repository;

import kaogongapp.demo.entity.FocusSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FocusSessionRepository extends JpaRepository<FocusSessionEntity, Long> {

    Optional<FocusSessionEntity> findBySessionIdAndUsernameAndActiveTrue(String sessionId, String username);

    List<FocusSessionEntity> findByUsernameAndActiveTrue(String username);
}

