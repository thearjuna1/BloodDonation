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

    @PostMapping
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<DonationDto.DonationResponse> scheduleDonation(
            @Valid @RequestBody DonationDto.CreateDonationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(donationService.scheduleDonation(request));
    }

    @PostMapping("/complete")
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<DonationDto.DonationResponse> completeDonation(
            @Valid @RequestBody DonationDto.CompleteDonationRequest request) {
        return ResponseEntity.ok(donationService.completeDonation(request));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<DonationDto.DonationResponse> cancelDonation(@PathVariable Long id) {
        return ResponseEntity.ok(donationService.cancelDonation(id));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<List<DonationDto.DonationResponse>> getMyDonations() {
        return ResponseEntity.ok(donationService.getMyDonations());
    }
}