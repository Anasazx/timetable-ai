package tn.rnu.isetmd.timetable.job;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.rnu.isetmd.timetable.storage.ImageStorageService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExtractionJobService {

    private final ExtractionJobRepository jobRepository;
    private final ImageStorageService imageStorageService;
    private final ExtractionJobWorker jobWorker;

    public ExtractionJob createJob(
            MultipartFile image
    ) throws IOException {

        UUID jobId = UUID.randomUUID();

        Path imagePath =
                imageStorageService.save(
                        jobId,
                        image
                );

        ExtractionJob job =
                new ExtractionJob(
                        jobId,
                        imagePath.toString()
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
                                "Extraction job not found: " + jobId
                        )
                );
    }
}