package bloodDonation.example.BloodDonation.service;

import bloodDonation.example.BloodDonation.dto.DonationDto;
import bloodDonation.example.BloodDonation.entity.BloodRequest;
import bloodDonation.example.BloodDonation.entity.Donation;
import bloodDonation.example.BloodDonation.entity.RequestStatus;
import bloodDonation.example.BloodDonation.entity.User;
import bloodDonation.example.BloodDonation.exception.BusinessException;
import bloodDonation.example.BloodDonation.repository.BloodRequestRepository;
import bloodDonation.example.BloodDonation.repository.DonationRepository;
import bloodDonation.example.BloodDonation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DonationService {

    private final DonationRepository donationRepository;
    private final BloodRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Value("${app.donation.min-gap-days:90}")
    private int minDonationGapDays;

    @Transactional
    public DonationDto.DonationResponse scheduleDonation(DonationDto.CreateDonationRequest dto) {
        User donor = getAuthenticatedUser();

        // Check 90-day eligibility
        if (!donor.isDonorEligible())
            throw new BusinessException("Not eligible yet. Next eligible: " + donor.getNextEligibleDonationDate());

        BloodRequest request = requestRepository.findById(dto.getBloodRequestId())
                .orElseThrow(() -> new BusinessException("Blood request not found"));

        // Check request is approved
        if (request.getStatus() != RequestStatus.APPROVED)
            throw new BusinessException("Donation can only be made for APPROVED requests");

        // Check blood group compatibility
        User.BloodGroup donorBlood = donor.getBloodGroup();
        User.BloodGroup requestedBlood = request.getBloodGroup();

        if (!BloodRequestService.canDonate(donorBlood, requestedBlood)) {
            throw new BusinessException(
                    "Incompatible blood group. Your blood type " +
                            donorBlood.name().replace("_", " ") +
                            " cannot donate to " +
                            requestedBlood.name().replace("_", " ")
            );
        }

        // Check no donation already scheduled for this request
        boolean hasActiveDonation = donationRepository
                .findByRequestIdAndStatusNot(request.getId(), Donation.DonationStatus.CANCELLED)
                .isPresent();

        if (hasActiveDonation)
            throw new BusinessException("A donation has already been scheduled for this request");
        Donation donation = Donation.builder()
                .donor(donor)
                .request(request)
                .unitsDonated(dto.getUnitsDonated())
                .build();

        Donation saved = donationRepository.save(donation);
        auditService.log("DONATION_SCHEDULED", donor.getId().toString(), saved.getId().toString());
        return mapToResponse(saved);
    }

    @Transactional
    public DonationDto.DonationResponse completeDonation(DonationDto.CompleteDonationRequest dto) {
        User donor = getAuthenticatedUser();
        Donation donation = getDonationById(dto.getDonationId());

        if (!donation.getDonor().getId().equals(donor.getId()))
            throw new BusinessException("You can only complete your own donations");

        if (donation.getStatus() != Donation.DonationStatus.SCHEDULED)
            throw new BusinessException("Donation must be in SCHEDULED state to complete");

        donation.setStatus(Donation.DonationStatus.COMPLETED);
        donation.setDonatedAt(LocalDateTime.now());

        BloodRequest request = donation.getRequest();
        request.setStatus(RequestStatus.DONATED);
        requestRepository.save(request);

        donor.setNextEligibleDonationDate(LocalDate.now().plusDays(minDonationGapDays));
        userRepository.save(donor);

        Donation saved = donationRepository.save(donation);
        auditService.log("DONATION_COMPLETED", donor.getId().toString(), saved.getId().toString());
        return mapToResponse(saved);
    }

    @Transactional
    public DonationDto.DonationResponse cancelDonation(Long donationId) {
        User donor = getAuthenticatedUser();
        Donation donation = getDonationById(donationId);

        if (!donation.getDonor().getId().equals(donor.getId()))
            throw new BusinessException("You can only cancel your own donations");
        if (donation.getStatus() == Donation.DonationStatus.COMPLETED)
            throw new BusinessException("Completed donations cannot be cancelled");

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

    private Donation getDonationById(Long id) {
        return donationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Donation not found: " + id));
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));
    }

    public DonationDto.DonationResponse mapToResponse(Donation d) {
        DonationDto.DonationResponse res = new DonationDto.DonationResponse();
        res.setId(d.getId());
        res.setDonorId(d.getDonor().getId());
        res.setDonorName(d.getDonor().getFullName());
        res.setBloodRequestId(d.getRequest().getId());
        res.setStatus(d.getStatus());
        res.setUnitsDonated(d.getUnitsDonated());
        res.setDonatedAt(d.getDonatedAt());
        res.setCreatedAt(d.getCreatedAt());
        return res;
    }
}