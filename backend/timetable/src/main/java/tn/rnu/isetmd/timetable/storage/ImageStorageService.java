package tn.rnu.isetmd.timetable.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class ImageStorageService {

    private final Path uploadDirectory =
            Paths.get("data/uploads/timetables")
                    .toAbsolutePath()
                    .normalize();

    public ImageStorageService() throws IOException {
        Files.createDirectories(uploadDirectory);
    }

    public Path save(UUID jobId, MultipartFile image)
            throws IOException {

        String originalFilename = image.getOriginalFilename();

        String extension = ".jpg";

        if (originalFilename != null) {

            int dotIndex = originalFilename.lastIndexOf('.');

            if (dotIndex >= 0) {
                extension = originalFilename.substring(dotIndex);
            }
        }

        Path destination =
                uploadDirectory.resolve(
                        jobId + extension
                );

        image.transferTo(destination);

        return destination;
    }
}