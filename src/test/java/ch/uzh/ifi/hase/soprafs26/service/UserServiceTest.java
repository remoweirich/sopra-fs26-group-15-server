package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs26.repository.UserAchievementRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UpdateUserPutDTO;
import ch.uzh.ifi.hase.soprafs26.security.AuthHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserService.
 * UserRepository is mocked to isolate business logic from the database.
 *
 * User data is now split: identity fields (username, email, password, bio)
 * live in the embedded UserProfile, while isOnline/isGuest are top-level booleans.
 * There is no longer a UserStatus enum.
 */
class UserServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long UNKNOWN_ID = 999L;
    private static final String USERNAME = "testUser";
    private static final String EMAIL = "test@uzh.ch";
    private static final String PASSWORD = "secret123";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAchievementRepository userAchievementRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        testUser = buildUser(USER_ID, USERNAME, EMAIL, PASSWORD);

        Mockito.when(userRepository.save(Mockito.any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ═══════════════════════════════════════════════════════════════════
    // registerUser
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: Neuer User mit gueltigen, einzigartigen Daten wird registriert.
     * Prueft: isOnline=false, isGuest=false, Scoreboard initialisiert mit 0 Punkten,
     * Token ist null (Token kommt erst beim Login), save() und flush() genau einmal.
     * Faengt Bug: Falsches isOnline/isGuest-Flag bricht Acceptance Criteria.
     * Fehlendes flush() laesst Daten bei Rollbacks verloren.
     */
    @Test
    void registerUser_validInput_setsDefaultsAndPersists() {
        Mockito.when(userRepository.findByUserProfileUsername(USERNAME)).thenReturn(null);
        Mockito.when(userRepository.findByUserProfileEmail(EMAIL)).thenReturn(null);

        User created = userService.registerUser(testUser);

        assertFalse(created.getIsOnline());
        assertFalse(created.getIsGuest());
        assertNotNull(created.getUserScoreboard());
        assertEquals(0L, created.getUserScoreboard().getTotalPoints());
        assertNull(created.getToken(),
                "Token must NOT be assigned at registration — it is set at login");
        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());
        Mockito.verify(userRepository, Mockito.times(1)).flush();
    }

    /**
     * Szenario: Username ist bereits vergeben.
     * Prueft: 400 BAD_REQUEST, save() wird NIE aufgerufen.
     * Faengt Bug: Wenn save() vor dem Uniqueness-Check aufgerufen wird
     * (Reihenfolge-Bug).
     */
    @Test
    void registerUser_duplicateUsername_throwsBadRequest() {
        Mockito.when(userRepository.findByUserProfileUsername(USERNAME)).thenReturn(testUser);
        Mockito.when(userRepository.findByUserProfileEmail(Mockito.anyString())).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.registerUser(testUser));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    /**
     * Szenario: Email ist bereits vergeben, Username nicht.
     * Prueft: 400 BAD_REQUEST, save() wird NIE aufgerufen.
     * Faengt Bug: Deckt den Email-Branch von checkIfUserExists() ab.
     */
    @Test
    void registerUser_duplicateEmail_throwsBadRequest() {
        Mockito.when(userRepository.findByUserProfileUsername(Mockito.anyString())).thenReturn(null);
        Mockito.when(userRepository.findByUserProfileEmail(EMAIL)).thenReturn(testUser);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.registerUser(testUser));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    /**
     * Szenario: Sowohl Username als auch Email bereits vergeben.
     * Prueft: 400 BAD_REQUEST, save() wird NIE aufgerufen.
     * Faengt Bug: Ersten kombinierten Branch von checkIfUserExists() ab.
     */
    @Test
    void registerUser_bothUsernameAndEmailDuplicate_throwsBadRequest() {
        Mockito.when(userRepository.findByUserProfileUsername(Mockito.anyString())).thenReturn(testUser);
        Mockito.when(userRepository.findByUserProfileEmail(Mockito.anyString())).thenReturn(testUser);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.registerUser(testUser));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    // ═══════════════════════════════════════════════════════════════════
    // loginUser
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: Registrierter User loggt sich mit korrektem Passwort ein.
     * Prueft: isOnline wird true, Token wird zugewiesen (nicht null),
     * save() und flush() werden aufgerufen.
     * Faengt Bug: Fehlendes save() wuerde den User nur in-memory ONLINE
     * setzen — beim naechsten AuthHeader-Check waere er wieder offline.
     */
    @Test
    void loginUser_validCredentials_setsOnlineTrueAndAssignsToken() {
        testUser.setIsOnline(false);
        testUser.setToken(null);
        Mockito.when(userRepository.findByUserProfileUsername(USERNAME)).thenReturn(testUser);
        Mockito.when(userRepository.findByToken(Mockito.anyString())).thenReturn(null);

        User logged = userService.loginUser(USERNAME, PASSWORD);

        assertTrue(logged.getIsOnline());
        assertNotNull(logged.getToken());
        Mockito.verify(userRepository, Mockito.times(1)).save(testUser);
        Mockito.verify(userRepository, Mockito.times(1)).flush();
        Mockito.verify(userRepository, Mockito.times(1)).findByToken(Mockito.anyString());
    }

    /**
     * Szenario: Login mit nicht existierendem Usernamen.
     * Prueft: 404 NOT_FOUND wird geworfen.
     * Faengt Bug: Fehlender null-Check wuerde NPE beim getUserProfile().getPassword()
     * fuehren statt sauberer 404.
     */
    @Test
    void loginUser_unknownUsername_throwsNotFound() {
        Mockito.when(userRepository.findByUserProfileUsername("ghost")).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.loginUser("ghost", "anything"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    /**
     * Szenario: User existiert, aber falsches Passwort.
     * Prueft: 401 UNAUTHORIZED, isOnline bleibt false, Token bleibt null.
     * Faengt Bug: SECURITY-KRITISCH. Invertierter Password-Check oder falsche
     * Reihenfolge wuerde isOnline/Token vor dem Check zuweisen.
     */
    @Test
    void loginUser_wrongPassword_throwsUnauthorizedAndKeepsUserOffline() {
        testUser.setIsOnline(false);
        testUser.setToken(null);
        Mockito.when(userRepository.findByUserProfileUsername(USERNAME)).thenReturn(testUser);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.loginUser(USERNAME, "WRONG_PASSWORD"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertFalse(testUser.getIsOnline());
        assertNull(testUser.getToken());
    }

    // ═══════════════════════════════════════════════════════════════════
    // getUserById
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: User existiert und wird per ID abgefragt.
     * Prueft: Der richtige User wird zurueckgegeben.
     * Faengt Bug: Partnert mit dem Error-Test — wenn jemand die Bedingung
     * invertiert, faellt dieser Happy-Path-Test sofort.
     */
    @Test
    void getUserById_existingUser_returnsUser() {
        Mockito.when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        User result = userService.getUserById(USER_ID);

        assertEquals(testUser.getUserId(), result.getUserId());
        assertEquals(USERNAME, result.getUserProfile().getUsername());
    }

    /**
     * Szenario: Abfrage einer User-ID, die nicht existiert.
     * Prueft: 404 NOT_FOUND wird geworfen.
     * Faengt Bug: Ein stillschweigendes 'return null' wuerde NPEs beim Caller
     * ausloesen statt einer sauberen 404.
     */
    @Test
    void getUserById_unknownId_throwsNotFound() {
        Mockito.when(userRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.getUserById(UNKNOWN_ID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ═══════════════════════════════════════════════════════════════════
    // updateUser
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: User aktualisiert NUR die Bio, andere Felder bleiben null im DTO.
     * Prueft: Bio wurde im UserProfile aktualisiert, Email/Password/Username
     * bleiben erhalten, save() wird aufgerufen.
     * Faengt Bug: Ohne null-Checks wuerde ein partielles DTO andere Felder in
     * UserProfile auf null ueberschreiben (HTTP-PUT-Semantik verletzt).
     */
    @Test
    void updateUser_partialUpdate_onlyChangesProvidedFieldsAndPersists() {
        testUser.getUserProfile().setUserBio("original bio");
        Mockito.when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        UpdateUserPutDTO dto = new UpdateUserPutDTO();
        dto.setUserBio("new bio");

        userService.updateUser(USER_ID, dto);

        assertEquals("new bio", testUser.getUserProfile().getUserBio());
        assertEquals(EMAIL, testUser.getUserProfile().getEmail());
        assertEquals(PASSWORD, testUser.getUserProfile().getPassword());
        assertEquals(USERNAME, testUser.getUserProfile().getUsername());
        Mockito.verify(userRepository, Mockito.times(1)).save(testUser);
    }

    /**
     * Szenario: Update-Versuch auf nicht existierende User-ID.
     * Prueft: 404 NOT_FOUND wird geworfen.
     * Faengt Bug: .orElse(null) statt .orElseThrow() wuerde eine NPE
     * ausloesen statt sauberer 404.
     */
    @Test
    void updateUser_unknownId_throwsNotFound() {
        Mockito.when(userRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.updateUser(UNKNOWN_ID, new UpdateUserPutDTO()));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ═══════════════════════════════════════════════════════════════════
    // logoutUser
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: Eingeloggter User (isOnline=true, Token gesetzt) loggt sich aus.
     * Prueft: Token wird null, isOnline wird false, save() wird aufgerufen.
     * Faengt Bug: Ungeloeschter Token koennte weiter fuer Requests genutzt werden
     * (Session-Fixation-Risiko). Fehlendes isOnline=false zeigt User als online.
     */
    @Test
    void logoutUser_validUser_clearsTokenAndSetsOffline() {
        testUser.setIsOnline(true);
        testUser.setToken("active-token");
        Mockito.when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        AuthHeader authHeader = new AuthHeader(USER_ID, "active-token");
        userService.logoutUser(authHeader);

        assertNull(testUser.getToken());
        assertFalse(testUser.getIsOnline());
        Mockito.verify(userRepository, Mockito.times(1)).save(testUser);
    }

    /**
     * Szenario: Logout-Versuch fuer einen User, dessen ID nicht existiert.
     * Prueft: 404 NOT_FOUND wird geworfen (Bug aus der alten Implementierung
     * ist gefixt — frueheres orElse(null) fuehrte zu NPE statt 404).
     */
    @Test
    void logoutUser_unknownUser_throwsNotFound() {
        Mockito.when(userRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        AuthHeader authHeader = new AuthHeader(UNKNOWN_ID, "any-token");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.logoutUser(authHeader));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ═══════════════════════════════════════════════════════════════════
    // createGuestUser
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: createGuestUser() legt einen anonymen User an und loggt ihn ein.
     * Prueft: Der zurueckgegebene User hat einen Token (loginUser wurde aufgerufen)
     * und einen Username mit "guest_"-Prefix.
     * Dokumentiert bekannten Bug: registerUser() setzt isGuest immer auf false,
     * auch wenn der Caller es vorher auf true gesetzt hat. Da createGuestUser()
     * registerUser() intern aufruft und kein zweites setIsGuest(true) danach
     * stattfindet, ist isGuest am Ende false.
     * TODO: registerUser() sollte isGuest respektieren wenn es bereits true ist.
     *       Sobald gefixt: assertFalse unten auf assertTrue aendern.
     */
    @Test
    void createGuestUser_returnsUserWithGuestPrefixAndTokenAndDocumentsBug() {
        // createGuestUser() calls registerUser() then loginUser() internally.
        // findByUserProfileUsername is invoked twice:
        //   1st call (checkIfUserExists inside registerUser) → must return null (no conflict)
        //   2nd call (inside loginUser) → must return the saved user so password check passes
        // We capture the saved user in savedGuest so both calls can share the same object.
        AtomicReference<User> savedGuest = new AtomicReference<>();

        Mockito.when(userRepository.findByUserProfileUsername(Mockito.anyString()))
                .thenReturn(null)                          // 1st call: uniqueness check → no conflict
                .thenAnswer(inv -> savedGuest.get());      // 2nd call: login lookup → saved user

        Mockito.when(userRepository.findByUserProfileEmail(Mockito.anyString())).thenReturn(null);
        Mockito.when(userRepository.findByToken(Mockito.anyString())).thenReturn(null);

        // Capture the saved user so loginUser can look it up (and compare the real password)
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setUserId(42L);
            savedGuest.set(u);
            return u;
        });

        User result = userService.createGuestUser();

        assertNotNull(result.getToken(), "Guest must have a token after createGuestUser()");
        assertTrue(result.getUserProfile().getUsername().startsWith("guest_"),
                "Guest username must start with 'guest_' prefix");

        assertTrue(result.getIsGuest(), "Guest user must have isGuest set to true");    }

    // ═══════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════

    private User buildUser(Long id, String username, String email, String password) {
        User user = new User();
        user.setUserId(id);
        UserProfile profile = new UserProfile();
        profile.setUsername(username);
        profile.setEmail(email);
        profile.setPassword(password);
        user.setUserProfile(profile);
        user.setIsOnline(false);
        user.setIsGuest(false);
        return user;
    }
}
