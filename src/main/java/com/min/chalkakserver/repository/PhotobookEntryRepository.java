package com.min.chalkakserver.repository;

import com.min.chalkakserver.entity.PhotobookEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PhotobookEntryRepository extends JpaRepository<PhotobookEntry, Long> {

    Page<PhotobookEntry> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<PhotobookEntry> findByUserIdAndFrameCollabIdOrderByCreatedAtDesc(
            Long userId, Long frameCollabId, Pageable pageable);

    long countByUserId(Long userId);

    @Query("SELECT pe.frameCollab.id AS collabId, COUNT(pe) AS photoCount " +
           "FROM PhotobookEntry pe " +
           "WHERE pe.user.id = :userId AND pe.frameCollab IS NOT NULL " +
           "GROUP BY pe.frameCollab.id")
    List<CollabPhotoCount> countByUserGroupedByCollab(@Param("userId") Long userId);

    interface CollabPhotoCount {
        Long getCollabId();
        Long getPhotoCount();
    }
}
