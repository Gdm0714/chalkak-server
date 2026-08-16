package com.min.chalkakserver.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "collab_candidates",
    indexes = {
        @Index(name = "idx_candidate_status_created_at", columnList = "status,created_at")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollabCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "raw_text", columnDefinition = "TEXT", nullable = false)
    private String rawText;

    @Column(length = 150)
    private String title;

    @Column(length = 50)
    private String brand;

    @Column(name = "artist_name", length = 100)
    private String artistName;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CandidateStatus status;

    @Column(name = "created_frame_collab_id")
    private Long createdFrameCollabId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public CollabCandidate(String sourceUrl, String rawText, String title, String brand,
                           String artistName, LocalDate startDate, LocalDate endDate) {
        this.sourceUrl = sourceUrl;
        this.rawText = rawText;
        this.title = title;
        this.brand = brand;
        this.artistName = artistName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = CandidateStatus.PENDING;
    }

    public void approve(Long frameCollabId) {
        this.status = CandidateStatus.APPROVED;
        this.createdFrameCollabId = frameCollabId;
    }

    public void reject() {
        this.status = CandidateStatus.REJECTED;
    }

    public enum CandidateStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
