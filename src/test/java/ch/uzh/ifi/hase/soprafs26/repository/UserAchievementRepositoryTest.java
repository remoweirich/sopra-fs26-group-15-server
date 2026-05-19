package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class UserAchievementRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    // ═══════════════════════════════════════════════════════════════════
    // findByUser
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: Alle UserAchievements eines bestimmten Users werden zurueckgegeben.
     * Faengt Bug: Wenn der User-Filter fehlt, wuerden Achievements anderer User
     * in der Profilanzeige erscheinen.
     */
    @Test
    public void findByUser_returnsAchievementsForThatUser() {
        User userA = persistUser("playerA", "a@uzh.ch");
        User userB = persistUser("playerB", "b@uzh.ch");
        Achievement a1 = persistAchievement("First Guess", "Made your first guess");
        Achievement a2 = persistAchievement("Sharpshooter", "Guessed within 1 km");

        persistUserAchievement(userA, a1);
        persistUserAchievement(userA, a2);
        persistUserAchievement(userB, a1);

        List<UserAchievement> result = userAchievementRepository.findByUser(userA);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(ua -> ua.getUser().getUserId().equals(userA.getUserId())));
    }

    /**
     * Prueft: Ein User ohne Achievements bekommt eine leere Liste (kein Crash).
     * Faengt Bug: Wenn ein NULL-Check fehlt, koennte die Profilseite crashen,
     * sobald ein neuer User noch keine Achievements hat.
     */
    @Test
    public void findByUser_userWithNoAchievements_returnsEmptyList() {
        User user = persistUser("noachieve", "noachieve@uzh.ch");

        List<UserAchievement> result = userAchievementRepository.findByUser(user);

        assertTrue(result.isEmpty());
    }

    /**
     * Prueft: Achievements verschiedener User werden korrekt isoliert —
     * User B's Achievements erscheinen nicht in User A's Liste.
     * Faengt Bug: Ein fehlender WHERE-Filter wuerde alle UserAchievements
     * zurueckgeben und jeder User haette scheinbar alle Achievements.
     */
    @Test
    public void findByUser_doesNotReturnAchievementsOfOtherUsers() {
        User userA = persistUser("isoA", "isoa@uzh.ch");
        User userB = persistUser("isoB", "isob@uzh.ch");
        Achievement badge = persistAchievement("Globetrotter", "Played in 5 lobbies");

        persistUserAchievement(userB, badge);

        List<UserAchievement> resultA = userAchievementRepository.findByUser(userA);

        assertTrue(resultA.isEmpty(), "userA must not see userB's achievements");
    }

    /**
     * Prueft: Das Feld unlockedAt wird automatisch beim Persist gesetzt (@PrePersist).
     * Faengt Bug: Wenn @PrePersist nicht greift, wuerde unlockedAt NULL sein und
     * ein NOT NULL-Constraint verletzt werden.
     */
    @Test
    public void findByUser_unlockedAtIsSetAutomatically() {
        User user = persistUser("timer", "timer@uzh.ch");
        Achievement badge = persistAchievement("Speedster", "Finished round in < 5 s");

        persistUserAchievement(user, badge);

        UserAchievement ua = userAchievementRepository.findByUser(user).get(0);

        assertNotNull(ua.getUnlockedAt(), "unlockedAt must be set by @PrePersist");
    }

    // ═══════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════

    private User persistUser(String username, String email) {
        User user = new User();
        UserProfile profile = new UserProfile();
        profile.setUsername(username);
        profile.setEmail(email);
        profile.setPassword("pw");
        user.setUserProfile(profile);
        user.setIsOnline(false);
        user.setIsGuest(false);
        user.setUserScoreboard(new UserScoreboard());
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    private Achievement persistAchievement(String name, String description) {
        Achievement a = new Achievement();
        a.setName(name);
        a.setDescription(description);
        a.setIconUrl("icons/" + name.toLowerCase().replace(" ", "_") + ".png");
        entityManager.persist(a);
        entityManager.flush();
        return a;
    }

    private UserAchievement persistUserAchievement(User user, Achievement achievement) {
        UserAchievement ua = new UserAchievement();
        ua.setUser(user);
        ua.setAchievement(achievement);
        entityManager.persist(ua);
        entityManager.flush();
        return ua;
    }
}
