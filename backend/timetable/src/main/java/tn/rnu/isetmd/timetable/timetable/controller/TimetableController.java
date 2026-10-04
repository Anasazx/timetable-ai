package tn.rnu.isetmd.timetable.timetable.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.rnu.isetmd.timetable.ai.dto.AiTimetableResponse;
import tn.rnu.isetmd.timetable.job.ExtractionJob;
import tn.rnu.isetmd.timetable.job.ExtractionJobService;
import tn.rnu.isetmd.timetable.job.dto.ExtractionJobResponse;
import tn.rnu.isetmd.timetable.job.dto.ExtractionJobStatusResponse;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/timetables")
@RequiredArgsConstructor
public class TimetableController {

    private final ExtractionJobService extractionJobService;
    private final ObjectMapper objectMapper;

    @PostMapping(
            value = "/extract",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ExtractionJobResponse> extractTimetable(
            @RequestParam("image") MultipartFile image
    ) throws IOException {

        ExtractionJob job =
                extractionJobService.createJob(image);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(
                        new ExtractionJobResponse(
                                job.getId(),
                                job.getStatus()
                        )
                );
    }

    @GetMapping(path = "/jobs/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ExtractionJobStatusResponse> getJobStatus(
            @PathVariable UUID jobId
    ) throws IOException {

        ExtractionJob job =
                extractionJobService.getJob(jobId);

        AiTimetableResponse result = null;

        if (job.getResultJson() != null) {

            result =
                    objectMapper.readValue(
                            job.getResultJson(),
                            AiTimetableResponse.class
                    );
        }

        return ResponseEntity.ok(
                new ExtractionJobStatusResponse(
                        job.getId(),
                        job.getStatus(),
                        result,
                        job.getErrorMessage()
                )
        );
    }
}