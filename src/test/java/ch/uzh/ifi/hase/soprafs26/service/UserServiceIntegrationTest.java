package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UpdateUserPutDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for UserService.
 *
 * Key difference to UserServiceTest (unit tests):
 * - No mocks. Uses the real UserRepository with an in-memory H2 database.
 * - @SpringBootTest boots the full application context including JPA.
 * - Tests exercise the full stack: Service -> JPA -> H2.
 *
 * This catches bugs that unit tests CANNOT catch:
 * - Unique constraint violations at the DB level
 * - JPA cascade behaviour (e.g. @Embedded UserScoreboard)
 * - Transaction and flush semantics
 * - Actual persistence of changes after save()
 *
 * @see UserService
 */
@WebAppConfiguration
@SpringBootTest
public class UserServiceIntegrationTest {

    @Qualifier("userRepository")
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @BeforeEach
    public void setup() {
        // Clean slate for each test — ensures tests are independent
        userRepository.deleteAll();
    }

    /**
     * Szenario: Ein neuer User wird registriert und in der ECHTEN H2-DB
     * gespeichert.
     * Prueft: Die zurueckgegebene userId ist von JPA auto-generiert (nicht null),
     * creationDate ist gesetzt, Status ist OFFLINE, und der User kann nach dem
     * Commit wieder ueber findByUsername() geladen werden (echte DB-Roundtrip).
     * Faengt Bug: Im Gegensatz zum Unit-Test fangen wir hier Bugs wie:
     * - @GeneratedValue vergessen (userId bleibt null)
     * - @Column(nullable = false) am falschen Feld (spart leise Daten aus)
     * - @Embedded UserScoreboard wird nicht mitgespeichert
     * - save() ohne flush() laesst Daten in der DB hinterher fehlen
     */
    @Test
    public void registerUser_validInput_persistsAndAutoAssignsId() {
        // given
        assertNull(userRepository.findByUsername("integrationUser"));

        User testUser = new User();
        testUser.setUsername("integrationUser");
        testUser.setEmail("integration@uzh.ch");
        testUser.setPassword("integrationPw");

        // when
        User created = userService.registerUser(testUser);

        // then
        assertNotNull(created.getUserId(), "JPA must auto-generate userId");
        assertEquals(UserStatus.OFFLINE, created.getStatus());
        assertNotNull(created.getCreationDate());
        assertNotNull(created.getUserScoreboard(),
                "@Embedded UserScoreboard must be persisted together with User");

        // Echte DB-Roundtrip: User kann ueber Username wieder geladen werden
        User loaded = userRepository.findByUsername("integrationUser");
        assertNotNull(loaded);
        assertEquals(created.getUserId(), loaded.getUserId());
    }

    /**
     * Szenario: Zwei User mit demselben Username werden nacheinander registriert.
     * Prueft: Der zweite Aufruf wirft eine ResponseStatusException, und der
     * zweite User wurde NICHT in die DB geschrieben (count bleibt 1).
     * Faengt Bug: Unit-Test prueft nur, dass die Service-Logik die Exception
     * wirft. Integration-Test prueft zusaetzlich, dass die DB den Zustand nicht
     * verschmutzt — z. B. koennte ein Bug save() trotz Exception aufrufen, und
     * die @Column(unique = true) auf Username wuerde erst auf DB-Ebene crashen
     * (StackTrace waere dann hässlich).
     */
    @Test
    public void registerUser_duplicateUsername_throwsAndDoesNotPersistSecondUser() {
        assertNull(userRepository.findByUsername("integrationUser"));

        User firstUser = new User();
        firstUser.setUsername("integrationUser");
        firstUser.setEmail("first@uzh.ch");
        firstUser.setPassword("pw1");
        userService.registerUser(firstUser);

        User duplicateUser = new User();
        duplicateUser.setUsername("integrationUser"); // same username
        duplicateUser.setEmail("second@uzh.ch");
        duplicateUser.setPassword("pw2");

        assertThrows(ResponseStatusException.class,
                () -> userService.registerUser(duplicateUser));

        // Die DB darf nur den ersten User enthalten
        assertEquals(1, userRepository.findAll().size());
    }

    /**
     * Szenario: Registrierter User loggt sich mit korrektem Passwort ein, dann
     * wird der User aus der echten DB erneut geladen.
     * Prueft: Nach loginUser() ist Status ONLINE und Token gesetzt — und diese
     * Aenderung ist in der DB persistiert (Reload zeigt denselben Token).
     * Faengt Bug: Ein fehlendes save()/flush() im loginUser() wuerde bei einem
     * Unit-Test nicht auffallen, weil Mocks keine Persistenz haben. Hier sehen
     * wir sofort, wenn der Token nur in-memory lebt und nicht wirklich
     * gespeichert wird — was in Produktion bedeuten wuerde: User loggt sich
     * ein, AuthHeader funktioniert beim naechsten Request nicht.
     */
    @Test
    public void loginUser_validCredentials_persistsOnlineStatusAndToken() {
        User testUser = new User();
        testUser.setUsername("loginIntegration");
        testUser.setEmail("login@uzh.ch");
        testUser.setPassword("correctPassword");
        User registered = userService.registerUser(testUser);

        User loggedIn = userService.loginUser("loginIntegration", "correctPassword");

        // Direkt nach dem Login
        assertEquals(UserStatus.ONLINE, loggedIn.getStatus());
        assertNotNull(loggedIn.getToken());

        // Reload aus echter DB: State ist persistiert
        User reloaded = userRepository.findById(registered.getUserId()).orElseThrow();
        assertEquals(UserStatus.ONLINE, reloaded.getStatus());
        assertEquals(loggedIn.getToken(), reloaded.getToken());
    }

    /**
     * Szenario: User-Update via partial PUT aendert nur die Bio, andere Felder
     * bleiben im DTO null.
     * Prueft: Nach updateUser() ist die Bio in der DB geaendert, aber Email und
     * Password sind unveraendert (HTTP-PUT-Semantik, wie vom Assignment
     * verlangt).
     * Faengt Bug: Der Unit-Test prueft das auf dem in-memory-User. Der
     * Integration-Test prueft das in der ECHTEN DB — hier wuerde ein fehlender
     * save()-Aufruf sofort sichtbar sein, weil der Reload die alten Daten
     * zurueckliefern wuerde.
     */
    @Test
    public void updateUser_partialUpdate_persistsOnlyChangedField() {
        User testUser = new User();
        testUser.setUsername("updateIntegration");
        testUser.setEmail("update@uzh.ch");
        testUser.setPassword("originalPw");
        testUser.setUserBio("original bio");
        User registered = userService.registerUser(testUser);

        UpdateUserPutDTO dto = new UpdateUserPutDTO();
        dto.setUserBio("updated bio");

        userService.updateUser(registered.getUserId(), dto);

        // Reload aus echter DB
        User reloaded = userRepository.findById(registered.getUserId()).orElseThrow();
        assertEquals("updated bio", reloaded.getUserBio());
        assertEquals("update@uzh.ch", reloaded.getEmail());
        assertEquals("originalPw", reloaded.getPassword());
        assertEquals("updateIntegration", reloaded.getUsername());
    }
}
