package bloodDonation.example.BloodDonation.repository;




import bloodDonation.example.BloodDonation.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("SELECT u FROM User u WHERE u.role = 'DONOR' AND u.adminVerified = true AND " +
            "(u.nextEligibleDonationDate IS NULL OR u.nextEligibleDonationDate <= CURRENT_DATE) AND " +
            "u.bloodGroup = :bloodGroup")
    List<User> findEligibleDonorsByBloodGroup(User.BloodGroup bloodGroup);

    @Query("SELECT u FROM User u WHERE u.role = 'DOCTOR' AND u.adminVerified = false")
    List<User> findPendingDoctorVerifications();

    List<User> findByRoleAndAccountLocked(User.Role role, boolean locked);
}