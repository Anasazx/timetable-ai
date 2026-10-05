package tn.rnu.isetmd.timetable.validation;

import org.springframework.stereotype.Service;
import tn.rnu.isetmd.timetable.ai.dto.AiTimetableEntry;
import tn.rnu.isetmd.timetable.ai.dto.AiTimetableResponse;
import tn.rnu.isetmd.timetable.ai.dto.TimetableValidationIssue;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TimetableValidationService {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    private static final Set<String> VALID_DAYS = Set.of(
            "Lundi",
            "Mardi",
            "Mercredi",
            "Jeudi",
            "Vendredi",
            "Samedi",
            "Dimanche"
    );

    public List<TimetableValidationIssue> validate(
            AiTimetableResponse timetable
    ) {

        List<TimetableValidationIssue> issues =
                new ArrayList<>();

        Set<String> seenEntries = new HashSet<>();

        List<AiTimetableEntry> entries =
                timetable.entries();

        for (int i = 0; i < entries.size(); i++) {

            AiTimetableEntry entry = entries.get(i);

            validateEntry(
                    i,
                    entry,
                    issues,
                    seenEntries
            );
        }

        validateOverlaps(entries, issues);

        return issues;
    }

    private void validateEntry(
            int index,
            AiTimetableEntry entry,
            List<TimetableValidationIssue> issues,
            Set<String> seenEntries
    ) {

        /*
         * DAY
         */

        if (entry.day() == null || entry.day().isBlank()) {

            addError(
                    issues,
                    index,
                    "MISSING_DAY",
                    "The day is missing."
            );

        } else if (!VALID_DAYS.contains(entry.day())) {

            addError(
                    issues,
                    index,
                    "INVALID_DAY",
                    "Invalid day: " + entry.day()
            );
        }


        /*
         * SUBJECT
         */

        if (entry.subject() == null || entry.subject().isBlank()) {

            addError(
                    issues,
                    index,
                    "MISSING_SUBJECT",
                    "The subject is missing."
            );

        } else if (looksIncomplete(entry.subject())) {

            addWarning(
                    issues,
                    index,
                    "SUSPICIOUS_SUBJECT",
                    "The subject name may be incomplete: "
                            + entry.subject()
            );
        }


        /*
         * TEACHER
         */

        if (entry.teacher() == null || entry.teacher().isBlank()) {

            addError(
                    issues,
                    index,
                    "MISSING_TEACHER",
                    "The teacher is missing."
            );
        }


        /*
         * ROOM
         */

        if (entry.room() == null || entry.room().isBlank()) {

            addError(
                    issues,
                    index,
                    "MISSING_ROOM",
                    "The room is missing."
            );
        }


        /*
         * TIME
         */

        LocalTime start = parseTime(
                index,
                entry.start_time(),
                "START_TIME",
                issues
        );

        LocalTime end = parseTime(
                index,
                entry.end_time(),
                "END_TIME",
                issues
        );

        if (start != null && end != null) {

            if (!start.isBefore(end)) {

                addError(
                        issues,
                        index,
                        "INVALID_TIME_RANGE",
                        "Start time must be before end time."
                );

            } else {

                long duration =
                        Duration.between(start, end).toMinutes();

                if (duration > 180) {

                    addWarning(
                            issues,
                            index,
                            "SUSPICIOUS_DURATION",
                            "Class duration is unusually long: "
                                    + duration
                                    + " minutes."
                    );
                }
            }
        }


        /*
         * DUPLICATE
         */

        String key =
                entry.day()
                        + "|"
                        + entry.start_time()
                        + "|"
                        + entry.end_time()
                        + "|"
                        + entry.subject()
                        + "|"
                        + entry.teacher()
                        + "|"
                        + entry.room();

        if (!seenEntries.add(key)) {

            addError(
                    issues,
                    index,
                    "DUPLICATE_ENTRY",
                    "This timetable entry appears to be duplicated."
            );
        }
    }


    private LocalTime parseTime(
            int index,
            String value,
            String field,
            List<TimetableValidationIssue> issues
    ) {

        if (value == null || value.isBlank()) {

            addError(
                    issues,
                    index,
                    "MISSING_" + field,
                    field + " is missing."
            );

            return null;
        }

        try {

            return LocalTime.parse(
                    value,
                    TIME_FORMAT
            );

        } catch (DateTimeParseException e) {

            addError(
                    issues,
                    index,
                    "INVALID_" + field,
                    "Invalid time format: " + value
            );

            return null;
        }
    }


    private void validateOverlaps(
            List<AiTimetableEntry> entries,
            List<TimetableValidationIssue> issues
    ) {

        for (int i = 0; i < entries.size(); i++) {

            AiTimetableEntry first =
                    entries.get(i);

            LocalTime firstStart =
                    parseTimeSilently(first.start_time());

            LocalTime firstEnd =
                    parseTimeSilently(first.end_time());

            if (firstStart == null || firstEnd == null) {
                continue;
            }

            for (int j = i + 1; j < entries.size(); j++) {

                AiTimetableEntry second =
                        entries.get(j);

                if (!first.day().equals(second.day())) {
                    continue;
                }

                LocalTime secondStart =
                        parseTimeSilently(second.start_time());

                LocalTime secondEnd =
                        parseTimeSilently(second.end_time());

                if (secondStart == null || secondEnd == null) {
                    continue;
                }

                boolean overlaps =
                        firstStart.isBefore(secondEnd)
                                && secondStart.isBefore(firstEnd);

                if (overlaps) {

                    addWarning(
                            issues,
                            j,
                            "OVERLAP",
                            "This entry overlaps with entry "
                                    + i
                                    + " on "
                                    + first.day()
                                    + "."
                    );
                }
            }
        }
    }


    private LocalTime parseTimeSilently(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {

            return LocalTime.parse(
                    value,
                    TIME_FORMAT
            );

        } catch (DateTimeParseException e) {

            return null;
        }
    }


    private boolean looksIncomplete(String subject) {

        String normalized =
                subject
                        .toLowerCase()
                        .trim();

        /*
         * Very simple first version.
         *
         * We will make this smarter later.
         */

        return normalized.endsWith(" empl")
                || normalized.endsWith(" d")
                || normalized.endsWith(" de")
                || normalized.endsWith(" du")
                || normalized.endsWith(" des");
    }


    public boolean hasErrors(
            List<TimetableValidationIssue> issues
    ) {

        return issues.stream()
                .anyMatch(issue ->
                        "ERROR".equals(issue.severity())
                );
    }


    private void addError(
            List<TimetableValidationIssue> issues,
            int index,
            String type,
            String message
    ) {

        issues.add(
                new TimetableValidationIssue(
                        index,
                        type,
                        "ERROR",
                        message
                )
        );
    }


    private void addWarning(
            List<TimetableValidationIssue> issues,
            int index,
            String type,
            String message
    ) {

        issues.add(
                new TimetableValidationIssue(
                        index,
                        type,
                        "WARNING",
                        message
                )
        );
    }
}