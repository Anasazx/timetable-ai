package tn.rnu.isetmd.timetable.ai.dto;

import java.util.List;

public record AiTimetableEntry(
        String day,
        String start_time,
        String end_time,
        String subject,
        String teacher,
        String room,
        String group,
        String type,
        List<String> uncertainties
) {
}