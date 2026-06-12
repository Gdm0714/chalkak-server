package com.min.chalkakserver.repository;

import com.min.chalkakserver.entity.FrameCollab;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FrameCollabRepository extends JpaRepository<FrameCollab, Long> {

    @Query("SELECT fc FROM FrameCollab fc " +
           "WHERE fc.startDate <= :today AND fc.endDate >= :today " +
           "ORDER BY fc.endDate ASC")
    Page<FrameCollab> findActive(@Param("today") LocalDate today, Pageable pageable);

    @Query("SELECT fc FROM FrameCollab fc " +
           "WHERE fc.startDate > :today " +
           "ORDER BY fc.startDate ASC")
    Page<FrameCollab> findUpcoming(@Param("today") LocalDate today, Pageable pageable);

    @Query("SELECT fc FROM FrameCollab fc " +
           "WHERE fc.endDate < :today " +
           "ORDER BY fc.endDate DESC")
    Page<FrameCollab> findEnded(@Param("today") LocalDate today, Pageable pageable);

    @Query("SELECT DISTINCT fc FROM FrameCollab fc " +
           "JOIN fc.photoBooths pb " +
           "WHERE pb.id = :photoBoothId AND fc.endDate >= :today " +
           "ORDER BY fc.startDate ASC")
    List<FrameCollab> findCurrentByPhotoBoothId(@Param("photoBoothId") Long photoBoothId,
                                                @Param("today") LocalDate today);

    @Query("SELECT fc FROM FrameCollab fc " +
           "LEFT JOIN FETCH fc.photoBooths " +
           "WHERE fc.id = :id")
    Optional<FrameCollab> findByIdWithBooths(@Param("id") Long id);
}
