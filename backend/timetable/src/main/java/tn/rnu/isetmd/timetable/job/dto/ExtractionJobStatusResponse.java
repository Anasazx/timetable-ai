package tn.rnu.isetmd.timetable.job.dto;

import tn.rnu.isetmd.timetable.ai.dto.AiTimetableResponse;

import tn.rnu.isetmd.timetable.ai.dto.TimetableValidationIssue;

import tn.rnu.isetmd.timetable.job.ExtractionJobStatus;

import java.time.LocalDate;

import java.util.List;

import java.util.UUID;

public record ExtractionJobStatusResponse(

        UUID jobId,

        ExtractionJobStatus status,

        LocalDate semesterStart,

        LocalDate semesterEnd,

        AiTimetableResponse result,

        List<TimetableValidationIssue> validationIssues,

        String error

) {

}