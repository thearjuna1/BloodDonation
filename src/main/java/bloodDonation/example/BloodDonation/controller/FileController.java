package bloodDonation.example.BloodDonation.controller;

import bloodDonation.example.BloodDonation.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @GetMapping("/{subfolder}/{filename}")
    public ResponseEntity<Resource> serveFile(
            @PathVariable String subfolder,
            @PathVariable String filename) {
        try {
            byte[] fileBytes = fileStorageService.loadFile(subfolder, filename);

            // Detect content type
            String contentType = "application/octet-stream";
            if (filename.endsWith(".png")) contentType = "image/png";
            else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) contentType = "image/jpeg";
            else if (filename.endsWith(".pdf")) contentType = "application/pdf";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(new ByteArrayResource(fileBytes));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}