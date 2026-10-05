package tn.rnu.isetmd.timetable.job;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.rnu.isetmd.timetable.ai.dto.AiTimetableResponse;
import tn.rnu.isetmd.timetable.ai.dto.TimetableValidationIssue;
import tn.rnu.isetmd.timetable.storage.ImageStorageService;
import tn.rnu.isetmd.timetable.validation.TimetableValidationService;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExtractionJobService {

    private final ExtractionJobRepository jobRepository;
    private final ImageStorageService imageStorageService;
    private final ExtractionJobWorker jobWorker;
    private final TimetableValidationService validationService;
    private final ObjectMapper objectMapper;

    public ExtractionJob createJob(
            MultipartFile image,
            LocalDate semesterStart,
            LocalDate semesterEnd
    ) throws IOException {

        UUID jobId = UUID.randomUUID();

        Path imagePath =
                imageStorageService.save(jobId, image);

        ExtractionJob job =
                new ExtractionJob(
                        jobId,
                        imagePath.toString(),
                        semesterStart,
                        semesterEnd
                );

        jobRepository.save(job);

        jobWorker.process(
                jobId,
                imagePath
        );

        return job;
    }

    public ExtractionJob getJob(UUID jobId) {

        return jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Extraction job not found: "
                                        + jobId
                        )
                );
    }

    public ExtractionJob confirmTimetable(
            UUID jobId,
            AiTimetableResponse timetable
    ) throws IOException {

        ExtractionJob job =
                jobRepository.findById(jobId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Extraction job not found: "
                                                + jobId
                                )
                        );

        if (job.getStatus()
                != ExtractionJobStatus.NEEDS_CONFIRMATION) {

            throw new IllegalStateException(
                    "This timetable is not waiting for confirmation."
            );
        }

        /*
         * Validate the user's corrected timetable.
         */

        List<TimetableValidationIssue> issues =
                validationService.validate(timetable);

        String resultJson =
                objectMapper.writeValueAsString(timetable);

        /*
         * Blocking errors still exist.
         * Keep the job waiting for user correction.
         */

        if (validationService.hasErrors(issues)) {

            job.needsConfirmation(
                    resultJson,
                    objectMapper.writeValueAsString(issues)
            );

            return jobRepository.save(job);
        }

        /*
         * No blocking errors.
         *
         * Warnings such as overlaps are acceptable
         * after user confirmation.
         */

        job.complete(resultJson);

        return jobRepository.save(job);
    }
}