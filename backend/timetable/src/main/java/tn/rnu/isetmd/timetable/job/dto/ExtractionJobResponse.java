package tn.rnu.isetmd.timetable.job.dto;

import tn.rnu.isetmd.timetable.job.ExtractionJobStatus;

import java.util.UUID;

public record ExtractionJobResponse(
        UUID jobId,
        ExtractionJobStatus status
) {
}
