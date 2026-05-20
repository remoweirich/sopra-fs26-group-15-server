package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUserProfileUsername(String username);
    User findByUserProfileEmail(String email);
    User findByToken(String token);

    @Query("SELECT u FROM User u WHERE u.isGuest = true")
    List<User> findAllGuests();

    List<User> findByUserProfile_UsernameContainingIgnoreCase(String username);

}

