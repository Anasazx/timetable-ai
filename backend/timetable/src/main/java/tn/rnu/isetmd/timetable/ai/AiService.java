package tn.rnu.isetmd.timetable.ai;

import tn.rnu.isetmd.timetable.ai.dto.AiTimetableResponse;

public interface AiService {
    AiTimetableResponse extractTimetable(byte[] image);
}