package tn.rnu.isetmd.timetable.job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExtractionJobRepository extends JpaRepository<ExtractionJob, UUID> {
}