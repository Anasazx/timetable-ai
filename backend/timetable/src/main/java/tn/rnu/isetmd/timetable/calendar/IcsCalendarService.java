package tn.rnu.isetmd.timetable.calendar;

import org.springframework.stereotype.Service;
import tn.rnu.isetmd.timetable.ai.dto.AiTimetableEntry;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class IcsCalendarService {

    private static final ZoneId TIME_ZONE =
            ZoneId.of("Africa/Tunis");

    private static final DateTimeFormatter ICS_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private static final DateTimeFormatter ICS_DATE_TIME_UTC =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private static final Map<String, DayOfWeek> DAY_MAPPING =
            Map.of(
                    "Lundi", DayOfWeek.MONDAY,
                    "Mardi", DayOfWeek.TUESDAY,
                    "Mercredi", DayOfWeek.WEDNESDAY,
                    "Jeudi", DayOfWeek.THURSDAY,
                    "Vendredi", DayOfWeek.FRIDAY,
                    "Samedi", DayOfWeek.SATURDAY,
                    "Dimanche", DayOfWeek.SUNDAY
            );

    /**
     * Generates a complete iCalendar file for the timetable.
     */
    public byte[] generate(
            List<AiTimetableEntry> entries,
            LocalDate semesterStart,
            LocalDate semesterEnd
    ) {

        StringBuilder ics = new StringBuilder();

        appendLine(ics, "BEGIN:VCALENDAR");
        appendLine(ics, "VERSION:2.0");
        appendLine(ics, "PRODID:-//Timetable AI//University Timetable//EN");
        appendLine(ics, "CALSCALE:GREGORIAN");
        appendLine(ics, "METHOD:PUBLISH");
        appendLine(ics, "X-WR-CALNAME:University Timetable");
        appendLine(ics, "X-WR-TIMEZONE:Africa/Tunis");

        for (AiTimetableEntry entry : entries) {

            appendEvent(
                    ics,
                    entry,
                    semesterStart,
                    semesterEnd
            );
        }

        appendLine(ics, "END:VCALENDAR");

        return ics
                .toString()
                .getBytes(StandardCharsets.UTF_8);
    }

    private void appendEvent(
            StringBuilder ics,
            AiTimetableEntry entry,
            LocalDate semesterStart,
            LocalDate semesterEnd
    ) {

        DayOfWeek dayOfWeek =
                DAY_MAPPING.get(entry.day());

        if (dayOfWeek == null) {
            throw new IllegalArgumentException(
                    "Invalid day: " + entry.day()
            );
        }

        LocalTime startTime =
                parseTime(entry.start_time());

        LocalTime endTime =
                parseTime(entry.end_time());

        LocalDate firstOccurrence =
                findFirstOccurrence(
                        semesterStart,
                        dayOfWeek
                );

        if (firstOccurrence.isAfter(semesterEnd)) {
            return;
        }

        ZonedDateTime start =
                ZonedDateTime.of(
                        firstOccurrence,
                        startTime,
                        TIME_ZONE
                );

        ZonedDateTime end =
                ZonedDateTime.of(
                        firstOccurrence,
                        endTime,
                        TIME_ZONE
                );

        String uid =
                generateUid(entry);


        appendLine(ics, "BEGIN:VEVENT");

        appendLine(
                ics,
                "UID:" + uid
        );

        appendLine(
                ics,
                "DTSTAMP:"
                        + ZonedDateTime.now(TIME_ZONE)
                        .withZoneSameInstant(
                                java.time.ZoneOffset.UTC
                        )
                        .format(ICS_DATE_TIME_UTC)
        );

        appendLine(
                ics,
                "DTSTART;TZID=Africa/Tunis:"
                        + start.format(ICS_DATE_TIME)
        );

        appendLine(
                ics,
                "DTEND;TZID=Africa/Tunis:"
                        + end.format(ICS_DATE_TIME)
        );

        appendLine(
                ics,
                "SUMMARY:"
                        + escapeText(entry.subject())
        );

        appendLine(
                ics,
                "LOCATION:"
                        + escapeText(entry.room())
        );

        appendLine(
                ics,
                "DESCRIPTION:"
                        + escapeText(
                        buildDescription(entry)
                )
        );

        appendLine(
                ics,
                "RRULE:FREQ=WEEKLY;UNTIL="
                        + getUntil(semesterEnd)
        );

        appendLine(
                ics,
                "END:VEVENT"
        );
    }

    private LocalDate findFirstOccurrence(
            LocalDate semesterStart,
            DayOfWeek targetDay
    ) {

        int daysUntil =
                targetDay.getValue()
                        - semesterStart.getDayOfWeek().getValue();

        if (daysUntil < 0) {
            daysUntil += 7;
        }

        return semesterStart.plusDays(daysUntil);
    }

    private String getUntil(LocalDate semesterEnd) {

        ZonedDateTime until =
                semesterEnd
                        .atTime(
                                LocalTime.MAX
                        )
                        .atZone(TIME_ZONE)
                        .withZoneSameInstant(
                                java.time.ZoneOffset.UTC
                        );

        return until.format(ICS_DATE_TIME_UTC);
    }

    private LocalTime parseTime(String value) {

        try {

            return LocalTime.parse(
                    value,
                    DateTimeFormatter.ofPattern("HH:mm")
            );

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "Invalid time: " + value,
                    e
            );
        }
    }

    private String buildDescription(
            AiTimetableEntry entry
    ) {

        StringBuilder description =
                new StringBuilder();

        if (entry.teacher() != null
                && !entry.teacher().isBlank()) {

            description
                    .append("Teacher: ")
                    .append(entry.teacher());
        }

        if (entry.type() != null
                && !entry.type().isBlank()) {

            if (!description.isEmpty()) {
                description.append("\n");
            }

            description
                    .append("Type: ")
                    .append(entry.type());
        }

        if (entry.group() != null
                && !entry.group().isBlank()) {

            if (!description.isEmpty()) {
                description.append("\n");
            }

            description
                    .append("Group: ")
                    .append(entry.group());
        }

        return description.toString();
    }

    /**
     * Generates a deterministic UID from the timetable entry.
     *
     * The same timetable entry will always receive the same UID.
     */
    private String generateUid(
            AiTimetableEntry entry
    ) {

        String value =
                String.join(
                        "|",
                        nullToEmpty(entry.day()),
                        nullToEmpty(entry.start_time()),
                        nullToEmpty(entry.end_time()),
                        nullToEmpty(entry.subject()),
                        nullToEmpty(entry.teacher()),
                        nullToEmpty(entry.room()),
                        nullToEmpty(entry.group()),
                        nullToEmpty(entry.type())
                );

        return sha256(value)
                + "@timetable-ai";
    }

    private String sha256(String value) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            value.getBytes(StandardCharsets.UTF_8)
                    );

            StringBuilder result =
                    new StringBuilder();

            for (byte b : hash) {

                result.append(
                        String.format(
                                "%02x",
                                b
                        )
                );
            }

            return result.toString();

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "SHA-256 is not available",
                    e
            );
        }
    }

    private String escapeText(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n");
    }

    private String nullToEmpty(String value) {

        return value == null ? "" : value;
    }

    private void appendLine(
            StringBuilder builder,
            String line
    ) {

        builder
                .append(line)
                .append("\r\n");
    }
}