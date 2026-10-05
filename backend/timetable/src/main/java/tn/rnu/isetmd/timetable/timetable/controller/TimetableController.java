package tn.rnu.isetmd.timetable.timetable.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.rnu.isetmd.timetable.ai.dto.AiTimetableResponse;
import tn.rnu.isetmd.timetable.ai.dto.TimetableValidationIssue;
import tn.rnu.isetmd.timetable.calendar.IcsCalendarService;
import tn.rnu.isetmd.timetable.job.ExtractionJob;
import tn.rnu.isetmd.timetable.job.ExtractionJobService;
import tn.rnu.isetmd.timetable.job.ExtractionJobStatus;
import tn.rnu.isetmd.timetable.job.dto.ExtractionJobResponse;
import tn.rnu.isetmd.timetable.job.dto.ExtractionJobStatusResponse;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/timetables")
@RequiredArgsConstructor
public class TimetableController {

    private final ExtractionJobService extractionJobService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExtractionJobResponse> extractTimetable(
            @RequestParam("image") MultipartFile image,
            @RequestParam("semesterStart")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate semesterStart,
            @RequestParam("semesterEnd")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate semesterEnd
    ) throws IOException {

        ExtractionJob job =
                extractionJobService.createJob(
                        image,
                        semesterStart,
                        semesterEnd
                );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(
                        new ExtractionJobResponse(
                                job.getId(),
                                job.getStatus()
                        )
                );
    }

    @GetMapping(
            path = "/jobs/{jobId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ExtractionJobStatusResponse> getJobStatus(
            @PathVariable UUID jobId
    ) throws IOException {

        ExtractionJob job =
                extractionJobService.getJob(jobId);

        return ResponseEntity.ok(
                buildJobStatusResponse(job)
        );
    }

    @PostMapping(
            path = "/jobs/{jobId}/confirm",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ExtractionJobStatusResponse> confirmTimetable(
            @PathVariable UUID jobId,
            @RequestBody AiTimetableResponse timetable
    ) throws IOException {

        ExtractionJob job = extractionJobService.confirmTimetable(jobId, timetable);

        return ResponseEntity.ok(buildJobStatusResponse(job));
    }




    private ExtractionJobStatusResponse buildJobStatusResponse(
            ExtractionJob job
    ) throws IOException {

        AiTimetableResponse result = null;

        if (job.getResultJson() != null) {

            result =
                    objectMapper.readValue(
                            job.getResultJson(),
                            AiTimetableResponse.class
                    );
        }


        List<TimetableValidationIssue> validationIssues =
                List.of();

        if (job.getValidationIssuesJson() != null) {

            validationIssues =
                    objectMapper.readValue(
                            job.getValidationIssuesJson(),
                            objectMapper.getTypeFactory()
                                    .constructCollectionType(
                                            List.class,
                                            TimetableValidationIssue.class
                                    )
                    );
        }


        return new ExtractionJobStatusResponse(
                job.getId(),
                job.getStatus(),
                job.getSemesterStart(),
                job.getSemesterEnd(),
                result,
                validationIssues,
                job.getErrorMessage()
        );
    }





    private final IcsCalendarService icsCalendarService;


    @GetMapping(
            path = "/jobs/{jobId}/calendar",
            produces = "text/calendar"
    )
    public ResponseEntity<byte[]> downloadCalendar(
            @PathVariable UUID jobId
    ) throws IOException {

        ExtractionJob job =
                extractionJobService.getJob(jobId);

        if (job.getStatus() != ExtractionJobStatus.COMPLETED) {

            throw new IllegalStateException(
                    "Calendar is only available for completed timetables."
            );
        }

        if (job.getResultJson() == null) {

            throw new IllegalStateException(
                    "Completed job has no timetable result."
            );
        }

        AiTimetableResponse timetable =
                objectMapper.readValue(
                        job.getResultJson(),
                        AiTimetableResponse.class
                );

        byte[] calendar =
                icsCalendarService.generate(
                        timetable.entries(),
                        job.getSemesterStart(),
                        job.getSemesterEnd()
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"timetable.ics\""
                )
                .header(
                        HttpHeaders.CONTENT_TYPE,
                        "text/calendar; charset=UTF-8"
                )
                .body(calendar);
    }




}