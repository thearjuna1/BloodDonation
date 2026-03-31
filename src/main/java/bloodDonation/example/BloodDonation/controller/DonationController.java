package bloodDonation.example.BloodDonation.controller;
import bloodDonation.example.BloodDonation.dto.DonationDto;
import bloodDonation.example.BloodDonation.service.DonationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;

    // Donor schedules a donation for an approved request
    @PostMapping
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<DonationDto.DonationResponse> scheduleDonation(
            @Valid @RequestBody DonationDto.CreateDonationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(donationService.scheduleDonation(request));
    }

    // Collector verifies themselves using OTP + Gov ID
    @PostMapping("/verify-collector")
    public ResponseEntity<DonationDto.DonationResponse> verifyCollector(
            @Valid @RequestBody DonationDto.CollectorVerifyRequest request) {
        return ResponseEntity.ok(donationService.verifyCollector(request));
    }

    // Donor marks donation as complete (after collector verified)
    @PostMapping("/complete")
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<DonationDto.DonationResponse> completeDonation(
            @Valid @RequestBody DonationDto.CompleteDonationRequest request) {
        return ResponseEntity.ok(donationService.completeDonation(request));
    }

    // Cancel a donation
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<DonationDto.DonationResponse> cancelDonation(@PathVariable Long id) {
        return ResponseEntity.ok(donationService.cancelDonation(id));
    }

    // View my donations (donor)
    @GetMapping("/my")
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<List<DonationDto.DonationResponse>> getMyDonations() {
        return ResponseEntity.ok(donationService.getMyDonations());
    }

    // Get donation for a specific request
    @GetMapping("/request/{requestId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<DonationDto.DonationResponse> getDonationByRequestId(
            @PathVariable Long requestId) {
        return ResponseEntity.ok(donationService.getDonationByRequestId(requestId));
    }
}
