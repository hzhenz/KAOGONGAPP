package kaogongapp.demo.repository;

import kaogongapp.demo.entity.AppUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUserEntity, Long> {

    Optional<AppUserEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    List<AppUserEntity> findAllByOrderByCreatedAtDesc();
}

