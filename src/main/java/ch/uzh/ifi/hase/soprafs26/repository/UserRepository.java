package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUserProfileUsername(String username);
    User findByUserProfileEmail(String email);
    User findByToken(String token);

    List<User> findByUserProfile_UsernameContainingIgnoreCase(String username);

}