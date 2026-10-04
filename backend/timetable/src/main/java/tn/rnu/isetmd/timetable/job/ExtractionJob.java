package tn.rnu.isetmd.timetable.job;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "extraction_jobs")
@Getter
@NoArgsConstructor
public class ExtractionJob {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExtractionJobStatus status;

    @Column(columnDefinition = "TEXT")
    private String resultJson;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private String imagePath;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public ExtractionJob(
            UUID id,
            String imagePath
    ) {
        this.id = id;
        this.imagePath = imagePath;
        this.status = ExtractionJobStatus.PROCESSING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void complete(String resultJson) {
        this.status = ExtractionJobStatus.COMPLETED;
        this.resultJson = resultJson;
        this.updatedAt = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        this.status = ExtractionJobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }
}