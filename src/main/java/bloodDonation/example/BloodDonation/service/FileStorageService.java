package bloodDonation.example.BloodDonation.service;

import bloodDonation.example.BloodDonation.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    public String storeFile(MultipartFile file, String subfolder) {
        try {
            Path uploadPath = Paths.get(uploadDir, subfolder);
            Files.createDirectories(uploadPath);

            String originalName = file.getOriginalFilename();
            String ext = (originalName != null && originalName.contains("."))
                    ? originalName.substring(originalName.lastIndexOf("."))
                    : "";
            String filename = UUID.randomUUID() + ext;

            Path targetPath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String url = "/api/files/" + subfolder + "/" + filename;
            log.info("File stored at: {}", targetPath);
            return url;

        } catch (IOException e) {
            log.error("File storage failed", e);
            throw new BusinessException("Failed to store file: " + e.getMessage());
        }
    }

    public byte[] loadFile(String subfolder, String filename) {
        try {
            Path filePath = Paths.get(uploadDir, subfolder, filename);
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new BusinessException("File not found: " + filename);
        }
    }
}