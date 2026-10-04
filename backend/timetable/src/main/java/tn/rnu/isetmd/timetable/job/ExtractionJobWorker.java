package tn.rnu.isetmd.timetable.job;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tn.rnu.isetmd.timetable.ai.AiService;
import tn.rnu.isetmd.timetable.ai.dto.AiTimetableResponse;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExtractionJobWorker {

    private final ExtractionJobRepository jobRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    @Async("timetableTaskExecutor")
    public void process(
            UUID jobId,
            Path imagePath
    ) {

        try {

            System.out.println(
                    "Starting timetable extraction job: " + jobId
            );

            byte[] image =
                    Files.readAllBytes(imagePath);

            AiTimetableResponse result =
                    aiService.extractTimetable(image);

            String resultJson =
                    objectMapper.writeValueAsString(result);

            ExtractionJob job =
                    jobRepository.findById(jobId)
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "Extraction job not found: " + jobId
                                    )
                            );

            job.complete(resultJson);

            jobRepository.save(job);

            System.out.println(
                    "Timetable extraction completed: " + jobId
            );

        } catch (Exception e) {

            System.err.println(
                    "Timetable extraction failed: " + jobId
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