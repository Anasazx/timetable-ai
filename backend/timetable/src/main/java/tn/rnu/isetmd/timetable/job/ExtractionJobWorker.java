package tn.rnu.isetmd.timetable.job;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tn.rnu.isetmd.timetable.ai.AiService;
import tn.rnu.isetmd.timetable.ai.dto.AiTimetableResponse;
import tn.rnu.isetmd.timetable.ai.dto.TimetableValidationIssue;
import tn.rnu.isetmd.timetable.validation.TimetableValidationService;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExtractionJobWorker {

    private final ExtractionJobRepository jobRepository;

    private final AiService aiService;

    private final TimetableValidationService validationService;

    private final ObjectMapper objectMapper;


    @Async("timetableTaskExecutor")
    public void process(
            UUID jobId,
            Path imagePath
    ) {

        try {

            System.out.println(
                    "Starting timetable extraction job: "
                            + jobId
            );

            byte[] image =
                    Files.readAllBytes(imagePath);


            /*
             * 1. AI EXTRACTION
             */

            AiTimetableResponse result =
                    aiService.extractTimetable(image);


            /*
             * 2. BACKEND VALIDATION
             */

            List<TimetableValidationIssue> issues =
                    validationService.validate(result);


            /*
             * 3. SERIALIZE RESULT
             */

            String resultJson =
                    objectMapper.writeValueAsString(result);


            /*
             * 4. UPDATE JOB
             */

            ExtractionJob job =
                    jobRepository.findById(jobId)
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "Extraction job not found: "
                                                    + jobId
                                    )
                            );


            if (!issues.isEmpty()) {

                /*
                 * We still store the extracted timetable.
                 *
                 * The frontend can show it to the user
                 * together with the validation problems.
                 */

                job.needsConfirmation(
                        resultJson,
                        objectMapper.writeValueAsString(issues)
                );

            } else {

                job.complete(resultJson);
            }


            jobRepository.save(job);


            System.out.println(
                    "Timetable extraction completed: "
                            + jobId
            );

        } catch (Exception e) {

            System.err.println(
                    "Timetable extraction failed: "
                            + jobId
            );

            e.printStackTrace();


            jobRepository.findById(jobId)
                    .ifPresent(job -> {

                        job.fail(
                                e.getMessage() != null
                                        ? e.getMessage()
                                        : "Unknown extraction error"
                        );

                        jobRepository.save(job);
                    });
        }
    }
}