package bloodDonation.example.BloodDonation.controller;

import bloodDonation.example.BloodDonation.dto.BloodRequestDto;
import bloodDonation.example.BloodDonation.service.BloodRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class BloodRequestController {

    private final BloodRequestService bloodRequestService;

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<BloodRequestDto.BloodRequestResponse> createRequest(
            @Valid @RequestBody BloodRequestDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bloodRequestService.createRequest(request));
    }

    @PostMapping("/{id}/documents")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<String> uploadDocuments(
            @PathVariable Long id,
            @RequestParam("prescription") MultipartFile prescription,
            @RequestParam("hospitalProof") MultipartFile hospitalProof) {

        bloodRequestService.uploadDocuments(id, prescription, hospitalProof);

        return ResponseEntity.ok("Documents uploaded successfully");
    }

    @PostMapping("/{id}/verify-documents")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<String> verifyDocuments(@PathVariable Long id) {

        bloodRequestService.verifyDocuments(id);

        return ResponseEntity.ok("Documents verified successfully");
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<BloodRequestDto.BloodRequestResponse> approveRequest(@PathVariable Long id) {
        return ResponseEntity.ok(bloodRequestService.approveRequest(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<BloodRequestDto.BloodRequestResponse> rejectRequest(
            @PathVariable Long id, @RequestParam String reason) {
        return ResponseEntity.ok(bloodRequestService.rejectRequest(id, reason));
    }

    @GetMapping("/my")
    public ResponseEntity<List<BloodRequestDto.BloodRequestResponse>> getMyRequests() {
        return ResponseEntity.ok(bloodRequestService.getMyRequests());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<List<BloodRequestDto.BloodRequestResponse>> getPendingRequests() {
        return ResponseEntity.ok(bloodRequestService.getPendingRequests());
    }

    // NEW — donors use this to find requests to donate to
    @GetMapping("/approved")
    @PreAuthorize("hasAnyRole('DONOR', 'ADMIN')")
    public ResponseEntity<List<BloodRequestDto.BloodRequestResponse>> getApprovedRequests() {
        return ResponseEntity.ok(bloodRequestService.getApprovedRequests());
    }
}