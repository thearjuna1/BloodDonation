package bloodDonation.example.BloodDonation.service;
import bloodDonation.example.BloodDonation.dto.DonationDto;
import bloodDonation.example.BloodDonation.entity.BloodRequest;
import bloodDonation.example.BloodDonation.entity.Donation;
import bloodDonation.example.BloodDonation.entity.User;
import bloodDonation.example.BloodDonation.exception.BusinessException;
import bloodDonation.example.BloodDonation.repository.BloodRequestRepository;
import bloodDonation.example.BloodDonation.repository.DonationRepository;
import bloodDonation.example.BloodDonation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DonationService {

    private final DonationRepository donationRepository;
    private final BloodRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final UserService userService;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.donation.min-gap-days}")
    private int minDonationGapDays;

    @Transactional
    public DonationDto.DonationResponse scheduleDonation(DonationDto.CreateDonationRequest dto) {
        User donor = getAuthenticatedUser();

        // ✅ Enforce 90-day donation gap
        if (!donor.isDonorEligible()) {
            throw new BusinessException(
                    "Donor not eligible yet. Next eligible date: " + donor.getNextEligibleDonationDate());
        }

        BloodRequest request = requestRepository.findById(dto.getBloodRequestId())
                .orElseThrow(() -> new BusinessException("Blood request not found"));

        // ✅ Only APPROVED requests can receive donations
        if (request.getStatus() != BloodRequest.RequestStatus.APPROVED) {
            throw new BusinessException("Donation can only be made for APPROVED blood requests");
        }

        // ✅ Prevent duplicate donations for same request
        if (donationRepository.findByBloodRequestId(request.getId()).isPresent()) {
            throw new BusinessException("A donation has already been scheduled for this request");
        }

        Donation donation = Donation.builder()
                .donor(donor)
                .bloodRequest(request)
                .unitsDonated(dto.getUnitsDonated())
                .build();

        Donation saved = donationRepository.save(donation);

        // Send OTP to collector for verification
        sendCollectorOtp(saved.getId());

        auditService.log("DONATION_SCHEDULED", donor.getId().toString(), saved.getId().toString());
        log.info("Donation {} scheduled by donor {}", saved.getId(), donor.getId());

        return mapToResponse(saved);
    }

    @Transactional
    public DonationDto.DonationResponse verifyCollector(DonationDto.CollectorVerifyRequest dto) {
        Donation donation = getDonationById(dto.getDonationId());

        if (donation.getStatus() != Donation.DonationStatus.SCHEDULED) {
            throw new BusinessException("Collector verification only allowed for SCHEDULED donations");
        }

        // Verify OTP
        String key = "collector_otp:" + dto.getDonationId();
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null) {
            throw new BusinessException("Collector OTP expired. Please request a new one.");
        }

        if (!storedOtp.equals(dto.getOtp())) {
            auditService.log("COLLECTOR_OTP_FAILED", "UNKNOWN", dto.getDonationId().toString());
            throw new BusinessException("Invalid OTP. Unauthorized collection attempt detected.");
        }

        // Hash and store government ID
        donation.setCollectorGovIdHash(userService.hashSensitiveData(dto.getGovId()));
        donation.setCollectorOtpVerified(true);
        donation.setStatus(Donation.DonationStatus.COLLECTOR_VERIFIED);

        redisTemplate.delete(key);

        Donation saved = donationRepository.save(donation);
        auditService.log("COLLECTOR_VERIFIED", "COLLECTOR", dto.getDonationId().toString());

        return mapToResponse(saved);
    }

    @Transactional
    public DonationDto.DonationResponse completeDonation(DonationDto.CompleteDonationRequest dto) {
        User donor = getAuthenticatedUser();
        Donation donation = getDonationById(dto.getDonationId());

        if (!donation.getDonor().getId().equals(donor.getId())) {
            throw new BusinessException("You can only complete your own donations");
        }

        // ✅ Collector must be verified before completion
        if (!donation.isCollectorVerified()) {
            throw new BusinessException(
                    "Donation cannot be completed: collector not verified. Anti-fraud check failed.");
        }

        if (donation.getStatus() != Donation.DonationStatus.COLLECTOR_VERIFIED) {
            throw new BusinessException("Donation must be in COLLECTOR_VERIFIED state to complete");
        }

        // Mark donation as completed
        donation.setStatus(Donation.DonationStatus.COMPLETED);
        donation.setDonatedAt(LocalDateTime.now());

        // ✅ Mark request as DONATED
        BloodRequest request = donation.getBloodRequest();
        request.setStatus(BloodRequest.RequestStatus.DONATED);
        requestRepository.save(request);

        // ✅ Update donor's next eligible date (90-day gap enforcement)
        donor.setNextEligibleDonationDate(LocalDate.now().plusDays(minDonationGapDays));
        userRepository.save(donor);

        Donation saved = donationRepository.save(donation);

        auditService.log("DONATION_COMPLETED", donor.getId().toString(), saved.getId().toString());
        log.info("Donation {} completed. Donor {} next eligible: {}",
                saved.getId(), donor.getId(), donor.getNextEligibleDonationDate());

        return mapToResponse(saved);
    }

    @Transactional
    public DonationDto.DonationResponse cancelDonation(Long donationId) {
        User donor = getAuthenticatedUser();
        Donation donation = getDonationById(donationId);

        if (!donation.getDonor().getId().equals(donor.getId())) {
            throw new BusinessException("You can only cancel your own donations");
        }

        if (donation.getStatus() == Donation.DonationStatus.COMPLETED) {
            throw new BusinessException("Completed donations cannot be cancelled");
        }

        donation.setStatus(Donation.DonationStatus.CANCELLED);
        Donation saved = donationRepository.save(donation);

        auditService.log("DONATION_CANCELLED", donor.getId().toString(), donationId.toString());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DonationDto.DonationResponse> getMyDonations() {
        User donor = getAuthenticatedUser();
        return donationRepository.findByDonorIdOrderByCreatedAtDesc(donor.getId())
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public DonationDto.DonationResponse getDonationByRequestId(Long requestId) {
        Donation donation = donationRepository.findByBloodRequestId(requestId)
                .orElseThrow(() -> new BusinessException("No donation found for request id: " + requestId));
        return mapToResponse(donation);
    }

    private void sendCollectorOtp(Long donationId) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        redisTemplate.opsForValue().set(
                "collector_otp:" + donationId,
                otp,
                30, TimeUnit.MINUTES
        );
        // In production: send via SMS/email to registered collector
        log.info("Collector OTP for donation {}: {}", donationId, otp);
    }

    private Donation getDonationById(Long id) {
        return donationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Donation not found with id: " + id));
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Authenticated user not found"));
    }

    public DonationDto.DonationResponse mapToResponse(Donation donation) {
        DonationDto.DonationResponse res = new DonationDto.DonationResponse();
        res.setId(donation.getId());
        res.setDonorId(donation.getDonor().getId());
        res.setDonorName(donation.getDonor().getFullName());
        res.setBloodRequestId(donation.getBloodRequest().getId());
        res.setStatus(donation.getStatus());
        res.setUnitsDonated(donation.getUnitsDonated());
        res.setCollectorVerified(donation.isCollectorVerified());
        res.setDonatedAt(donation.getDonatedAt());
        res.setCreatedAt(donation.getCreatedAt());
        return res;
    }
}
