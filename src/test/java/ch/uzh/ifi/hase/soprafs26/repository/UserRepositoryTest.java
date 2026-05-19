package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs26.entity.UserScoreboard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    // ═══════════════════════════════════════════════════════════════════
    // findByUserProfileUsername
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: Ein persistierter User kann ueber seinen Username gefunden werden.
     * Faengt Bug: Wenn der JPQL-Ableitungspfad "UserProfile_Username" falsch
     * ist oder der Column-Name in UserProfile nicht mit dem Feldnamen uebereinstimmt.
     */
    @Test
    public void findByUserProfileUsername_existingUser_returnsUser() {
        User user = persistUser("alice", "alice@uzh.ch", "pw");

        User found = userRepository.findByUserProfileUsername("alice");

        assertNotNull(found);
        assertEquals(user.getUserId(), found.getUserId());
        assertEquals("alice", found.getUserProfile().getUsername());
    }

    /**
     * Prueft: Abfrage eines nicht vorhandenen Usernames liefert null.
     * Faengt Bug: Ein fehlerhafter Query wuerde immer den ersten User
     * zurueckgeben statt null.
     */
    @Test
    public void findByUserProfileUsername_unknownUsername_returnsNull() {
        persistUser("bob", "bob@uzh.ch", "pw");

        User found = userRepository.findByUserProfileUsername("ghost");

        assertNull(found);
    }

    // ═══════════════════════════════════════════════════════════════════
    // findByUserProfileEmail
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: Ein persistierter User kann ueber seine Email gefunden werden.
     * Faengt Bug: Wenn der Pfad "UserProfile_Email" im abgeleiteten Query
     * falsch ist, wuerde die Duplicate-Email-Pruefung in registerUser() nie
     * greifen und doppelte Emails wuerden akzeptiert.
     */
    @Test
    public void findByUserProfileEmail_existingUser_returnsUser() {
        User user = persistUser("carol", "carol@uzh.ch", "pw");

        User found = userRepository.findByUserProfileEmail("carol@uzh.ch");

        assertNotNull(found);
        assertEquals(user.getUserId(), found.getUserId());
    }

    /**
     * Prueft: Unbekannte Email liefert null.
     */
    @Test
    public void findByUserProfileEmail_unknownEmail_returnsNull() {
        User found = userRepository.findByUserProfileEmail("nobody@uzh.ch");

        assertNull(found);
    }

    // ═══════════════════════════════════════════════════════════════════
    // findByToken
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: Ein User kann ueber seinen Token gefunden werden.
     * Faengt Bug: loginUser() prueft Token-Uniqueness ueber findByToken() —
     * ein kaputter Query wuerde doppelte Tokens zulassen.
     */
    @Test
    public void findByToken_existingToken_returnsUser() {
        User user = persistUser("dave", "dave@uzh.ch", "pw");
        user.setToken("unique-token-xyz");
        entityManager.flush();

        User found = userRepository.findByToken("unique-token-xyz");

        assertNotNull(found);
        assertEquals(user.getUserId(), found.getUserId());
    }

    /**
     * Prueft: Unbekannter Token liefert null (Uniqueness-Schleife terminiert).
     * Faengt Bug: Wenn findByToken null nie zurueckgibt, wuerde die
     * Token-Generierungsschleife in loginUser() unendlich laufen.
     */
    @Test
    public void findByToken_unknownToken_returnsNull() {
        User found = userRepository.findByToken("nonexistent-token");

        assertNull(found);
    }

    // ═══════════════════════════════════════════════════════════════════
    // Helper
    // ═══════════════════════════════════════════════════════════════════

    private User persistUser(String username, String email, String password) {
        User user = new User();
        UserProfile profile = new UserProfile();
        profile.setUsername(username);
        profile.setEmail(email);
        profile.setPassword(password);
        user.setUserProfile(profile);
        user.setIsOnline(false);
        user.setIsGuest(false);
        user.setUserScoreboard(new UserScoreboard());
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }
}
