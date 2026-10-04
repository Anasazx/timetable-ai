package tn.rnu.isetmd.timetable.job.dto;

import tn.rnu.isetmd.timetable.ai.dto.AiTimetableResponse;
import tn.rnu.isetmd.timetable.job.ExtractionJobStatus;

import java.util.UUID;

public record ExtractionJobStatusResponse(
        UUID jobId,
        ExtractionJobStatus status,
        AiTimetableResponse result,
        String error
) {
}