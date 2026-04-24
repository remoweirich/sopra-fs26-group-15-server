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

    // Constants for consistent test fixtures across all tests
    private static final String REGISTER_USERNAME = "integrationUser";
    private static final String REGISTER_EMAIL = "integration@uzh.ch";
    private static final String REGISTER_PASSWORD = "integrationPw";

    private static final String LOGIN_USERNAME = "loginIntegration";
    private static final String LOGIN_EMAIL = "login@uzh.ch";
    private static final String LOGIN_PASSWORD = "correctPassword";

    private static final String UPDATE_USERNAME = "updateIntegration";
    private static final String UPDATE_EMAIL = "update@uzh.ch";
    private static final String UPDATE_PASSWORD = "originalPw";
    private static final String UPDATE_ORIGINAL_BIO = "original bio";
    private static final String UPDATE_NEW_BIO = "updated bio";

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
     * creationDate ist gesetzt, Status ist OFFLINE, UserScoreboard wurde
     * mitpersistiert, und der User kann nach dem Commit wieder ueber
     * findByUsername() geladen werden (echte DB-Roundtrip).
     * Faengt Bug: Im Gegensatz zum Unit-Test fangen wir hier Bugs wie:
     * - @GeneratedValue vergessen (userId bleibt null)
     * - @Column(nullable = false) am falschen Feld
     * - @Embedded UserScoreboard wird nicht mitgespeichert
     * - save() ohne flush() laesst Daten in der DB hinterher fehlen
     */
    @Test
    public void registerUser_validInput_persistsAndAutoAssignsId() {
        // given
        assertNull(userRepository.findByUsername(REGISTER_USERNAME),
                "Precondition: no user with this username exists yet");

        User testUser = new User();
        testUser.setUsername(REGISTER_USERNAME);
        testUser.setEmail(REGISTER_EMAIL);
        testUser.setPassword(REGISTER_PASSWORD);

        // when
        User created = userService.registerUser(testUser);

        // then
        assertNotNull(created.getUserId(), "JPA must auto-generate userId");
        assertEquals(UserStatus.OFFLINE, created.getStatus());
        assertNotNull(created.getCreationDate());
        assertNotNull(created.getUserScoreboard(),
                "@Embedded UserScoreboard must be persisted together with User");

        // Echte DB-Roundtrip: User kann ueber Username wieder geladen werden
        User loaded = userRepository.findByUsername(REGISTER_USERNAME);
        assertNotNull(loaded, "User must be findable in DB after registerUser()");
        assertEquals(created.getUserId(), loaded.getUserId(),
                "Loaded user must have the same ID as the created user");
    }

    /**
     * Szenario: Zwei User mit demselben Username werden nacheinander registriert.
     * Prueft: Der zweite Aufruf wirft eine ResponseStatusException, und es
     * befindet sich GENAU ein User in der DB (der erste) — der zweite wurde
     * nicht geschrieben.
     * Faengt Bug: Unit-Test prueft nur, dass die Service-Logik die Exception
     * wirft. Integration-Test prueft zusaetzlich, dass die DB den Zustand nicht
     * verschmutzt — z. B. koennte ein Bug save() trotz Exception aufrufen, und
     * die @Column(unique = true) auf Username wuerde erst auf DB-Ebene crashen
     * mit einer haesslichen ConstraintViolation statt einer sauberen 400.
     */
    @Test
    public void registerUser_duplicateUsername_throwsAndDoesNotPersistSecondUser() {
        assertNull(userRepository.findByUsername(REGISTER_USERNAME));

        User firstUser = new User();
        firstUser.setUsername(REGISTER_USERNAME);
        firstUser.setEmail("first@uzh.ch");
        firstUser.setPassword("pw1");
        userService.registerUser(firstUser);

        User duplicateUser = new User();
        duplicateUser.setUsername(REGISTER_USERNAME); // same username, different email
        duplicateUser.setEmail("second@uzh.ch");
        duplicateUser.setPassword("pw2");

        assertThrows(ResponseStatusException.class,
                () -> userService.registerUser(duplicateUser));

        // Die DB darf NUR den ersten User enthalten
        assertEquals(1, userRepository.findAll().size(),
                "DB must contain exactly one user after failed duplicate registration");
    }

    /**
     * Szenario: Registrierter User loggt sich mit korrektem Passwort ein, dann
     * wird der User aus der echten DB erneut geladen.
     * Prueft: Nach loginUser() ist Status ONLINE und Token gesetzt — und diese
     * Aenderung ist tatsaechlich in der DB persistiert (Reload zeigt denselben
     * Token und denselben Status).
     * Faengt Bug: Ein fehlendes save()/flush() im loginUser() wuerde beim
     * Unit-Test nicht auffallen, weil Mocks keine Persistenz haben. Hier sehen
     * wir sofort, wenn der Token nur in-memory lebt und nicht wirklich
     * gespeichert wird — was in Produktion bedeutet: User loggt sich ein,
     * AuthHeader-Check beim naechsten Request schlaegt fehl, de facto
     * Login-kaputt.
     */
    @Test
    public void loginUser_validCredentials_persistsOnlineStatusAndToken() {
        User testUser = new User();
        testUser.setUsername(LOGIN_USERNAME);
        testUser.setEmail(LOGIN_EMAIL);
        testUser.setPassword(LOGIN_PASSWORD);
        User registered = userService.registerUser(testUser);

        User loggedIn = userService.loginUser(LOGIN_USERNAME, LOGIN_PASSWORD);

        // Direkt nach dem Login: Return-Wert hat korrekten State
        assertEquals(UserStatus.ONLINE, loggedIn.getStatus());
        assertNotNull(loggedIn.getToken());

        // Reload aus echter DB: State ist tatsaechlich persistiert
        User reloaded = userRepository.findById(registered.getUserId()).orElseThrow(
                () -> new AssertionError("User disappeared from DB after login"));
        assertEquals(UserStatus.ONLINE, reloaded.getStatus(),
                "ONLINE status must survive a DB reload");
        assertEquals(loggedIn.getToken(), reloaded.getToken(),
                "Token must survive a DB reload");
    }

    /**
     * Szenario: User-Update via partial PUT aendert nur die Bio, andere Felder
     * bleiben im DTO null.
     * Prueft: Nach updateUser() ist die Bio in der DB geaendert, aber Email,
     * Password und Username sind unveraendert (HTTP-PUT-Semantik, wie vom
     * Assignment verlangt).
     * Faengt Bug: Der Unit-Test prueft das auf dem in-memory-User. Der
     * Integration-Test prueft das in der ECHTEN DB — hier wuerde ein fehlender
     * save()-Aufruf sofort sichtbar sein, weil der Reload die alten Daten
     * zurueckliefern wuerde.
     */
    @Test
    public void updateUser_partialUpdate_persistsOnlyChangedField() {
        User testUser = new User();
        testUser.setUsername(UPDATE_USERNAME);
        testUser.setEmail(UPDATE_EMAIL);
        testUser.setPassword(UPDATE_PASSWORD);
        testUser.setUserBio(UPDATE_ORIGINAL_BIO);
        User registered = userService.registerUser(testUser);

        UpdateUserPutDTO dto = new UpdateUserPutDTO();
        dto.setUserBio(UPDATE_NEW_BIO);

        userService.updateUser(registered.getUserId(), dto);

        // Reload aus echter DB: Aenderung ist persistiert, andere Felder unberuehrt
        User reloaded = userRepository.findById(registered.getUserId()).orElseThrow(
                () -> new AssertionError("User disappeared from DB after update"));
        assertEquals(UPDATE_NEW_BIO, reloaded.getUserBio(),
                "Bio must be updated in DB");
        assertEquals(UPDATE_EMAIL, reloaded.getEmail(),
                "Email must be unchanged (was not in update DTO)");
        assertEquals(UPDATE_PASSWORD, reloaded.getPassword(),
                "Password must be unchanged (was not in update DTO)");
        assertEquals(UPDATE_USERNAME, reloaded.getUsername(),
                "Username must be unchanged (was not in update DTO)");
    }
}
