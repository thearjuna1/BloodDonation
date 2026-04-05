package bloodDonation.example.BloodDonation.service;

import bloodDonation.example.BloodDonation.dto.BloodRequestDto;
import bloodDonation.example.BloodDonation.entity.BloodRequest;
import bloodDonation.example.BloodDonation.entity.RequestStatus;
import bloodDonation.example.BloodDonation.entity.User;
import bloodDonation.example.BloodDonation.exception.BusinessException;
import bloodDonation.example.BloodDonation.repository.BloodRequestRepository;
import bloodDonation.example.BloodDonation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BloodRequestService {

    private final BloodRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final FileStorageService fileStorageService;

    // Blood group compatibility — donor blood group -> list of blood groups it can donate to
    private static final Map<User.BloodGroup, List<User.BloodGroup>> COMPATIBILITY = Map.of(
            User.BloodGroup.O_NEG,  List.of(
                    User.BloodGroup.O_NEG, User.BloodGroup.O_POS,
                    User.BloodGroup.A_NEG, User.BloodGroup.A_POS,
                    User.BloodGroup.B_NEG, User.BloodGroup.B_POS,
                    User.BloodGroup.AB_NEG, User.BloodGroup.AB_POS),
            User.BloodGroup.O_POS,  List.of(
                    User.BloodGroup.O_POS, User.BloodGroup.A_POS,
                    User.BloodGroup.B_POS, User.BloodGroup.AB_POS),
            User.BloodGroup.A_NEG,  List.of(
                    User.BloodGroup.A_NEG, User.BloodGroup.A_POS,
                    User.BloodGroup.AB_NEG, User.BloodGroup.AB_POS),
            User.BloodGroup.A_POS,  List.of(
                    User.BloodGroup.A_POS, User.BloodGroup.AB_POS),
            User.BloodGroup.B_NEG,  List.of(
                    User.BloodGroup.B_NEG, User.BloodGroup.B_POS,
                    User.BloodGroup.AB_NEG, User.BloodGroup.AB_POS),
            User.BloodGroup.B_POS,  List.of(
                    User.BloodGroup.B_POS, User.BloodGroup.AB_POS),
            User.BloodGroup.AB_NEG, List.of(
                    User.BloodGroup.AB_NEG, User.BloodGroup.AB_POS),
            User.BloodGroup.AB_POS, List.of(
                    User.BloodGroup.AB_POS)
    );

    public static boolean canDonate(User.BloodGroup donor, User.BloodGroup recipient) {
        List<User.BloodGroup> compatible = COMPATIBILITY.get(donor);
        return compatible != null && compatible.contains(recipient);
    }

    @Transactional
    public BloodRequestDto.BloodRequestResponse createRequest(BloodRequestDto.CreateRequest dto) {
        User patient = getAuthenticatedUser();

        BloodRequest request = BloodRequest.builder()
                .patient(patient)
                .bloodGroup(dto.getBloodGroup())
                .hospitalName(dto.getHospitalName())
                .hospitalAddress(dto.getHospitalAddress())
                .unitsRequired(dto.getUnitsRequired())
                .status(RequestStatus.PENDING)
                .build();

        BloodRequest saved = requestRepository.save(request);
        auditService.log("REQUEST_CREATED", patient.getId().toString(), saved.getId().toString());
        return mapToResponse(saved);
    }

    @Transactional
    public void uploadDocuments(Long requestId, MultipartFile prescription, MultipartFile hospitalProof) {
        BloodRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException("Request not found"));

        String presUrl = fileStorageService.storeFile(prescription, "prescriptions");
        String hospUrl = fileStorageService.storeFile(hospitalProof, "hospital");

        request.setPrescriptionDocUrl(presUrl);
        request.setHospitalProofUrl(hospUrl);
        request.setHasDocuments(true);
        requestRepository.save(request);
    }

    @Transactional
    public void verifyDocuments(Long requestId) {
        BloodRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException("Request not found"));
        request.setDocumentsVerified(true);
        requestRepository.save(request);
    }

    @Transactional
    public BloodRequestDto.BloodRequestResponse approveRequest(Long requestId) {
        User doctor = getAuthenticatedUser();
        BloodRequest request = getRequestById(requestId);
        request.setStatus(RequestStatus.APPROVED);
        BloodRequest saved = requestRepository.save(request);
        auditService.log("REQUEST_APPROVED", doctor.getId().toString(), requestId.toString());
        return mapToResponse(saved);
    }

    @Transactional
    public BloodRequestDto.BloodRequestResponse rejectRequest(Long requestId, String reason) {
        BloodRequest request = getRequestById(requestId);
        request.setStatus(RequestStatus.REJECTED);
        request.setRejectionReason(reason);
        BloodRequest saved = requestRepository.save(request);
        auditService.log("REQUEST_REJECTED", "SYSTEM", requestId.toString());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BloodRequestDto.BloodRequestResponse> getMyRequests() {
        User user = getAuthenticatedUser();
        return requestRepository.findByPatientIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BloodRequestDto.BloodRequestResponse> getPendingRequests() {
        return requestRepository.findByStatus(RequestStatus.PENDING)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BloodRequestDto.BloodRequestResponse> getApprovedRequests() {
        User donor = getAuthenticatedUser();
        User.BloodGroup donorBlood = donor.getBloodGroup();

        return requestRepository.findByStatus(RequestStatus.APPROVED)
                .stream()
                // Only show requests this donor's blood group is compatible with
                .filter(r -> canDonate(donorBlood, r.getBloodGroup()))
                .map(this::mapToResponse)
                .toList();
    }

    private BloodRequest getRequestById(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Blood request not found: " + id));
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));
    }

    public BloodRequestDto.BloodRequestResponse mapToResponse(BloodRequest r) {
        BloodRequestDto.BloodRequestResponse res = new BloodRequestDto.BloodRequestResponse();
        res.setId(r.getId());
        res.setPatientId(r.getPatient().getId());
        res.setPatientName(r.getPatient().getFullName());
        res.setBloodGroup(r.getBloodGroup());
        res.setStatus(r.getStatus());
        res.setHospitalName(r.getHospitalName());
        res.setHospitalAddress(r.getHospitalAddress());
        res.setUnitsRequired(r.getUnitsRequired());
        res.setHasDocuments(r.isHasDocuments());
        res.setDocumentsVerified(r.isDocumentsVerified());
        res.setPrescriptionDocUrl(r.getPrescriptionDocUrl());
        res.setHospitalProofUrl(r.getHospitalProofUrl());
        res.setRejectionReason(r.getRejectionReason());
        res.setCreatedAt(r.getCreatedAt());
        res.setUpdatedAt(r.getUpdatedAt());
        return res;
    }
}