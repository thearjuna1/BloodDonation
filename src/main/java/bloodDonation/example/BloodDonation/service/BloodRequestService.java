package bloodDonation.example.BloodDonation.service;

import bloodDonation.example.BloodDonation.dto.BloodRequestDto;
import bloodDonation.example.BloodDonation.entity.BloodRequest;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class BloodRequestService {

    private final BloodRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    // Fraud detection: max 3 requests per hour per patient
    private static final long MAX_REQUESTS_PER_HOUR = 3;

    @Transactional
    public BloodRequestDto.BloodRequestResponse createRequest(BloodRequestDto.CreateRequest dto) {
        User patient = getAuthenticatedUser();

        // Fraud detection: throttle excessive requests
        long recentCount = requestRepository.countRecentRequestsByPatient(patient.getId());
        if (recentCount >= MAX_REQUESTS_PER_HOUR) {
            auditService.log("FRAUD_TOO_MANY_REQUESTS", patient.getId().toString(), "SYSTEM");
            throw new BusinessException("Too many requests submitted in a short period. Please wait.");
        }

        BloodRequest request = BloodRequest.builder()
                .patient(patient)
                .bloodGroup(dto.getBloodGroup())
                .hospitalName(dto.getHospitalName())
                .hospitalAddress(dto.getHospitalAddress())
                .unitsRequired(dto.getUnitsRequired())
                .build();

        BloodRequest saved = requestRepository.save(request);
        auditService.log("REQUEST_CREATED", patient.getId().toString(), saved.getId().toString());

        return mapToResponse(saved);
    }

    @Transactional
    public BloodRequestDto.BloodRequestResponse uploadDocuments(
            Long requestId, MultipartFile prescription, MultipartFile hospitalProof) {

        BloodRequest request = getRequestById(requestId);
        User patient = getAuthenticatedUser();

        if (!request.getPatient().getId().equals(patient.getId())) {
            throw new BusinessException("You can only upload documents for your own requests");
        }

        if (request.getStatus() != BloodRequest.RequestStatus.PENDING) {
            throw new BusinessException("Documents can only be uploaded for PENDING requests");
        }

        // In production: upload to S3/Cloudinary and store URL
        // Simulated here — replace with actual cloud storage service
        String prescriptionUrl = "/uploads/" + requestId + "/prescription_" + prescription.getOriginalFilename();
        String hospitalProofUrl = "/uploads/" + requestId + "/hospital_" + hospitalProof.getOriginalFilename();

        request.setPrescriptionDocUrl(prescriptionUrl);
        request.setHospitalProofUrl(hospitalProofUrl);

        BloodRequest saved = requestRepository.save(request);
        auditService.log("DOCUMENTS_UPLOADED", patient.getId().toString(), requestId.toString());

        return mapToResponse(saved);
    }

    @Transactional
    public BloodRequestDto.BloodRequestResponse approveRequest(Long requestId) {
        User doctor = getAuthenticatedUser();

        // Enforce: only verified doctors can approve
        if (!doctor.isVerifiedDoctor()) {
            throw new BusinessException("Only admin-verified doctors can approve blood requests");
        }

        BloodRequest request = getRequestById(requestId);

        // Enforce the trust rule: no docs = no approval
        if (!request.canBeApproved()) {
            throw new BusinessException(
                    "Request cannot be approved: documents must be uploaded and verified first");
        }

        request.setStatus(BloodRequest.RequestStatus.APPROVED);
        request.setApprovedBy(doctor);

        BloodRequest saved = requestRepository.save(request);
        auditService.log("REQUEST_APPROVED", doctor.getId().toString(), requestId.toString());

        log.info("Request {} approved by doctor {}", requestId, doctor.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public BloodRequestDto.BloodRequestResponse rejectRequest(Long requestId, String reason) {
        User doctor = getAuthenticatedUser();

        if (!doctor.isVerifiedDoctor()) {
            throw new BusinessException("Only admin-verified doctors can reject blood requests");
        }

        BloodRequest request = getRequestById(requestId);

        if (request.getStatus() != BloodRequest.RequestStatus.PENDING) {
            throw new BusinessException("Only PENDING requests can be rejected");
        }

        request.setStatus(BloodRequest.RequestStatus.REJECTED);
        request.setRejectionReason(reason);

        BloodRequest saved = requestRepository.save(request);
        auditService.log("REQUEST_REJECTED", doctor.getId().toString(), requestId.toString());

        return mapToResponse(saved);
    }

    @Transactional
    public BloodRequestDto.BloodRequestResponse verifyDocuments(Long requestId) {
        User doctor = getAuthenticatedUser();

        if (!doctor.isVerifiedDoctor()) {
            throw new BusinessException("Only verified doctors can verify documents");
        }

        BloodRequest request = getRequestById(requestId);

        if (request.getPrescriptionDocUrl() == null || request.getHospitalProofUrl() == null) {
            throw new BusinessException("Documents have not been uploaded yet");
        }

        request.setDocumentsVerified(true);
        BloodRequest saved = requestRepository.save(request);
        auditService.log("DOCUMENTS_VERIFIED", doctor.getId().toString(), requestId.toString());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BloodRequestDto.BloodRequestResponse> getMyRequests() {
        User user = getAuthenticatedUser();
        return requestRepository.findByPatientIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BloodRequestDto.BloodRequestResponse> getPendingRequests() {
        return requestRepository.findVerifiedPendingRequests()
                .stream().map(this::mapToResponse).toList();
    }

    private BloodRequest getRequestById(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Blood request not found with id: " + id));
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Authenticated user not found"));
    }

    public BloodRequestDto.BloodRequestResponse mapToResponse(BloodRequest request) {
        BloodRequestDto.BloodRequestResponse res = new BloodRequestDto.BloodRequestResponse();
        res.setId(request.getId());
        res.setPatientId(request.getPatient().getId());
        res.setPatientName(request.getPatient().getFullName());
        res.setBloodGroup(request.getBloodGroup());
        res.setStatus(request.getStatus());
        res.setHospitalName(request.getHospitalName());
        res.setHospitalAddress(request.getHospitalAddress());
        res.setUnitsRequired(request.getUnitsRequired());
        res.setDocumentsVerified(request.isDocumentsVerified());
        res.setRejectionReason(request.getRejectionReason());
        res.setCreatedAt(request.getCreatedAt());
        res.setUpdatedAt(request.getUpdatedAt());
        return res;
    }
}
