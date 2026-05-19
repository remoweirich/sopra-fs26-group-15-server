package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UpdateUserPutDTO;
import ch.uzh.ifi.hase.soprafs26.security.AuthHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for UserService.
 *
 * Key difference to UserServiceTest (unit tests):
 * - No mocks. Uses the real UserRepository with an in-memory H2 database.
 * - @SpringBootTest boots the full application context including JPA.
 * - Tests exercise the full stack: Service → JPA → H2.
 *
 * User structure changed: username/email/password/bio now live inside the
 * embedded UserProfile. isOnline replaces the old UserStatus enum.
 * logoutUser() NPE bug (orElse(null) without null-check) is now fixed —
 * it properly throws 404 for unknown users.
 */
@WebAppConfiguration
@SpringBootTest
public class UserServiceIntegrationTest {

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
    private LobbyRepository lobbyRepository;

    @Autowired
    private UserService userService;

    @BeforeEach
    public void setup() {
        // Lobbies reference users via FK — delete child rows first
        lobbyRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * Szenario: Neuer User wird registriert und in der echten H2-DB gespeichert.
     * Prueft: userId ist JPA-auto-generiert (nicht null), creationDate gesetzt
     * (@PrePersist), isOnline=false, UserScoreboard mitpersistiert, und der User
     * kann nach dem Commit ueber findByUserProfileUsername() geladen werden.
     * Faengt Bug: @GeneratedValue vergessen, @Embedded UserScoreboard fehlt,
     * save() ohne flush() laesst Daten nach Commit fehlen.
     */
    @Test
    public void registerUser_validInput_persistsAndAutoAssignsId() {
        assertNull(userRepository.findByUserProfileUsername(REGISTER_USERNAME),
                "Precondition: no user with this username exists yet");

        User testUser = buildUser(REGISTER_USERNAME, REGISTER_EMAIL, REGISTER_PASSWORD);
        User created = userService.registerUser(testUser);

        assertNotNull(created.getUserId(), "JPA must auto-generate userId");
        assertFalse(created.getIsOnline());
        assertNotNull(created.getCreationDate(), "@PrePersist must set creationDate");
        assertNotNull(created.getUserScoreboard(),
                "@Embedded UserScoreboard must be persisted together with User");

        // Echte DB-Roundtrip
        User loaded = userRepository.findByUserProfileUsername(REGISTER_USERNAME);
        assertNotNull(loaded, "User must be findable in DB after registerUser()");
        assertEquals(created.getUserId(), loaded.getUserId());
    }

    /**
     * Szenario: Zwei User mit demselben Username werden nacheinander registriert.
     * Prueft: Der zweite Aufruf wirft eine ResponseStatusException, und es
     * befindet sich GENAU ein User in der DB.
     * Faengt Bug: Unit-Test prueft Service-Logik; Integration-Test prueft
     * zusaetzlich, dass die DB nicht mit einem halbfertigen User verschmutzt wird.
     */
    @Test
    public void registerUser_duplicateUsername_throwsAndDoesNotPersistSecondUser() {
        assertNull(userRepository.findByUserProfileUsername(REGISTER_USERNAME));

        userService.registerUser(buildUser(REGISTER_USERNAME, "first@uzh.ch", "pw1"));

        User duplicate = buildUser(REGISTER_USERNAME, "second@uzh.ch", "pw2");
        assertThrows(ResponseStatusException.class,
                () -> userService.registerUser(duplicate));

        assertEquals(1, userRepository.findAll().size(),
                "DB must contain exactly one user after failed duplicate registration");
    }

    /**
     * Szenario: Registrierter User loggt sich ein, danach wird der User neu
     * aus der DB geladen.
     * Prueft: isOnline=true und Token sind nach dem Reload noch vorhanden
     * (tatsaechlich persistiert, nicht nur in-memory).
     * Faengt Bug: Fehlendes save()/flush() im loginUser() wuerde nur in-memory
     * wirken — naechster AuthHeader-Check schlaegt fehl.
     */
    @Test
    public void loginUser_validCredentials_persistsOnlineStatusAndToken() {
        User registered = userService.registerUser(
                buildUser(LOGIN_USERNAME, LOGIN_EMAIL, LOGIN_PASSWORD));

        User loggedIn = userService.loginUser(LOGIN_USERNAME, LOGIN_PASSWORD);

        assertTrue(loggedIn.getIsOnline());
        assertNotNull(loggedIn.getToken());

        // Reload aus echter DB: State ist tatsaechlich persistiert
        User reloaded = userRepository.findById(registered.getUserId()).orElseThrow(
                () -> new AssertionError("User disappeared from DB after login"));
        assertTrue(reloaded.getIsOnline(),
                "isOnline=true must survive a DB reload");
        assertEquals(loggedIn.getToken(), reloaded.getToken(),
                "Token must survive a DB reload");
    }

    /**
     * Szenario: User-Update aendert nur die Bio; andere Felder bleiben null im DTO.
     * Prueft: Bio ist nach Reload in der DB geaendert, aber Email, Password und
     * Username sind unveraendert (HTTP-PUT-Semantik).
     * Faengt Bug: Der Unit-Test prueft das auf dem in-memory-User. Der
     * Integration-Test zeigt, ob das save() wirklich persistiert.
     */
    @Test
    public void updateUser_partialUpdate_persistsOnlyChangedField() {
        User testUser = buildUser(UPDATE_USERNAME, UPDATE_EMAIL, UPDATE_PASSWORD);
        testUser.getUserProfile().setUserBio(UPDATE_ORIGINAL_BIO);
        User registered = userService.registerUser(testUser);

        UpdateUserPutDTO dto = new UpdateUserPutDTO();
        dto.setUserBio(UPDATE_NEW_BIO);
        userService.updateUser(registered.getUserId(), dto);

        User reloaded = userRepository.findById(registered.getUserId()).orElseThrow(
                () -> new AssertionError("User disappeared from DB after update"));
        assertEquals(UPDATE_NEW_BIO, reloaded.getUserProfile().getUserBio(),
                "Bio must be updated in DB");
        assertEquals(UPDATE_EMAIL, reloaded.getUserProfile().getEmail(),
                "Email must be unchanged");
        assertEquals(UPDATE_PASSWORD, reloaded.getUserProfile().getPassword(),
                "Password must be unchanged");
        assertEquals(UPDATE_USERNAME, reloaded.getUserProfile().getUsername(),
                "Username must be unchanged");
    }

    /**
     * Szenario: Eingeloggter User loggt sich aus; User wird danach aus DB geladen.
     * Prueft: Token ist null und isOnline=false in der DB (nicht nur in-memory).
     * Faengt Bug: Fehlendes save() in logoutUser() wuerde den Token nur
     * in-memory loeschen — der naechste AuthHeader-Check wuerde den alten Token
     * als gueltig akzeptieren.
     */
    @Test
    public void logoutUser_validUser_persistsClearedTokenAndOfflineStatus() {
        User registered = userService.registerUser(
                buildUser("logoutUser", "logout@uzh.ch", "logoutPw"));
        User loggedIn = userService.loginUser("logoutUser", "logoutPw");
        assertNotNull(loggedIn.getToken());

        userService.logoutUser(new AuthHeader(loggedIn.getUserId(), loggedIn.getToken()));

        User reloaded = userRepository.findById(loggedIn.getUserId()).orElseThrow(
                () -> new AssertionError("User disappeared from DB after logout"));
        assertNull(reloaded.getToken(), "Token must be null in DB after logout");
        assertFalse(reloaded.getIsOnline(), "isOnline must be false in DB after logout");
    }

    /**
     * Szenario: createGuestUser() legt einen anonymen User in der echten DB an.
     * Prueft: Guest-User ist in der DB persistiert, hat Token, Username mit
     * "guest_"-Prefix.
     * Dokumentiert bekannten Bug: registerUser() setzt isGuest immer auf false,
     * deshalb ist isGuest nach createGuestUser() false statt true.
     * TODO: registerUser() sollte isGuest respektieren wenn es bereits gesetzt ist.
     *       Sobald gefixt: assertFalse auf assertTrue aendern.
     */
    @Test
    public void createGuestUser_persistsGuestButLosesIsGuestFlag_knownBug() {
        User guest = userService.createGuestUser();

        assertNotNull(guest.getUserId(), "Guest user must be persisted");

        User loaded = userRepository.findById(guest.getUserId()).orElseThrow(
                () -> new AssertionError("Guest user was not persisted to the database"));

        assertTrue(loaded.getUserProfile().getUsername().startsWith("guest_"),
                "Guest username must start with 'guest_' prefix");
        assertNotNull(loaded.getToken(), "Guest must have a token");

        // KNOWN BUG: registerUser() overrides isGuest to false even when caller sets true.
        assertFalse(loaded.getIsGuest(),
                "BUG: isGuest should be true — flip to assertTrue once registerUser() is fixed.");
    }

    // ═══════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════

    private User buildUser(String username, String email, String password) {
        User user = new User();
        UserProfile profile = new UserProfile();
        profile.setUsername(username);
        profile.setEmail(email);
        profile.setPassword(password);
        user.setUserProfile(profile);
        return user;
    }
}
