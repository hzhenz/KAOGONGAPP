package kaogongapp.demo.repository;

import kaogongapp.demo.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<JobEntity, Long> {
}
