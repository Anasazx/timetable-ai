package tn.rnu.isetmd.timetable.ai.dto;

public record TimetableValidationIssue(
        int entryIndex,
        String type,
        String severity,
        String message
) {}