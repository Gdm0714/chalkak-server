package com.min.chalkakserver.repository;

import com.min.chalkakserver.entity.CollabCandidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollabCandidateRepository extends JpaRepository<CollabCandidate, Long> {

    Page<CollabCandidate> findByStatusOrderByCreatedAtDesc(
            CollabCandidate.CandidateStatus status, Pageable pageable);

    Page<CollabCandidate> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
