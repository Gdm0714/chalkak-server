package com.min.chalkakserver.repository;

import com.min.chalkakserver.entity.PhotoBoothReport;
import com.min.chalkakserver.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhotoBoothReportRepository extends JpaRepository<PhotoBoothReport, Long> {

    @Query("SELECT r FROM PhotoBoothReport r WHERE r.user = :user ORDER BY r.createdAt DESC")
    List<PhotoBoothReport> findByUser(@Param("user") User user);

    void deleteAllByUser(User user);
}
