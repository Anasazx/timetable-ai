package tn.rnu.isetmd.timetable.ai.dto;

import java.util.List;
public record AiTimetableResponse(
        List<AiTimetableEntry> entries,
        List<String> questions_for_user
) {}