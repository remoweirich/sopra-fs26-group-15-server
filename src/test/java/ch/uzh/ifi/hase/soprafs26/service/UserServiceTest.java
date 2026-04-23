package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserService.
 * UserRepository is mocked to isolate business logic from the database.
 */
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("testUser");
        testUser.setEmail("test@uzh.ch");
        testUser.setPassword("secret123");

        // save(user) returns the same user, mimicking JPA behaviour.
        Mockito.when(userRepository.save(Mockito.any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // --- registerUser ---

    /**
     * Szenario: Neuer User mit gueltigen, einzigartigen Daten wird registriert.
     * Prueft: Status OFFLINE, creationDate gesetzt, isGuest=false, Scoreboard
     * initialisiert mit 0 Punkten, save() und flush() genau einmal aufgerufen.
     * Faengt Bug: Vergessenes setStatus/setCreationDate bricht Acceptance
     * Criteria aus S1. Fehlendes flush() laesst Daten bei Rollbacks verloren.
     */
    @Test
    void registerUser_validInput_setsDefaultsAndPersists() {
        Mockito.when(userRepository.findByUsername("testUser")).thenReturn(null);
        Mockito.when(userRepository.findByEmail("test@uzh.ch")).thenReturn(null);

        User created = userService.registerUser(testUser);

        assertEquals(UserStatus.OFFLINE, created.getStatus());
        assertNotNull(created.getCreationDate());
        assertFalse(created.getIsGuest());
        assertNotNull(created.getUserScoreboard());
        assertEquals(0, created.getUserScoreboard().getTotalPoints());
        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());
        Mockito.verify(userRepository, Mockito.times(1)).flush();
    }

    /**
     * Szenario: Username ist bereits vergeben, Email nicht.
     * Prueft: 400 BAD_REQUEST wird geworfen, save() wird NIE aufgerufen.
     * Faengt Bug: Wenn die Validierung umgangen wird oder save() schon vor
     * dem Check ausgefuehrt wurde (Reihenfolge-Bug).
     */
    @Test
    void registerUser_duplicateUsername_throwsBadRequest() {
        Mockito.when(userRepository.findByUsername("testUser")).thenReturn(testUser);
        Mockito.when(userRepository.findByEmail(Mockito.anyString())).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.registerUser(testUser));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    // --- loginUser ---

    /**
     * Szenario: Registrierter User loggt sich mit korrektem Passwort ein.
     * Prueft: Status wechselt auf ONLINE, Token wird zugewiesen.
     * Faengt Bug: Vergessenes setStatus zeigt User als offline in der UI,
     * vergessenes setToken macht Zugriff auf geschuetzte Endpoints unmoeglich.
     */
    @Test
    void loginUser_validCredentials_setsStatusOnlineAndAssignsToken() {
        testUser.setStatus(UserStatus.OFFLINE);
        testUser.setToken(null);
        Mockito.when(userRepository.findByUsername("testUser")).thenReturn(testUser);
        Mockito.when(userRepository.findByToken(Mockito.anyString())).thenReturn(null);

        User logged = userService.loginUser("testUser", "secret123");

        assertEquals(UserStatus.ONLINE, logged.getStatus());
        assertNotNull(logged.getToken());
    }

    /**
     * Szenario: Login-Versuch mit nicht existierendem Usernamen.
     * Prueft: 404 NOT_FOUND wird geworfen.
     * Faengt Bug: Fehlender null-Check wuerde zu NullPointerException beim
     * naechsten .getPassword()-Aufruf fuehren statt zu sauberer 404.
     */
    @Test
    void loginUser_unknownUsername_throwsNotFound() {
        Mockito.when(userRepository.findByUsername("ghost")).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.loginUser("ghost", "anything"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    /**
     * Szenario: User existiert, aber falsches Passwort wird eingegeben.
     * Prueft: 401 UNAUTHORIZED, Status bleibt OFFLINE, Token bleibt null.
     * Faengt Bug: SECURITY-KRITISCH. Ein invertierter Password-Check ('.equals'
     * statt '!equals') wuerde jedes Passwort akzeptieren. Falsche Reihenfolge
     * wuerde Status/Token schon vor dem Check zuweisen.
     */
    @Test
    void loginUser_wrongPassword_throwsUnauthorizedAndKeepsUserOffline() {
        testUser.setStatus(UserStatus.OFFLINE);
        testUser.setToken(null);
        Mockito.when(userRepository.findByUsername("testUser")).thenReturn(testUser);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.loginUser("testUser", "WRONG_PASSWORD"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals(UserStatus.OFFLINE, testUser.getStatus());
        assertNull(testUser.getToken());
    }

    // --- getUserById ---

    /**
     * Szenario: Abfrage einer User-ID, die nicht existiert.
     * Prueft: 404 NOT_FOUND wird geworfen.
     * Faengt Bug: Assignment verlangt in User Story S2 explizit 404. Ein
     * stillschweigendes 'return null' wuerde die REST-Spec brechen.
     */
    @Test
    void getUserById_unknownId_throwsNotFound() {
        Mockito.when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.getUserById(999L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // --- updateUser ---

    /**
     * Szenario: User aktualisiert NUR die Bio, andere Felder bleiben null im DTO.
     * Prueft: Bio wurde aktualisiert, Email/Password/Username bleiben erhalten.
     * Faengt Bug: Klassischer HTTP-PUT-Bug. Das Assignment verlangt explizit,
     * dass nur uebergebene Felder aktualisiert werden. Ohne null-Checks wuerde
     * z.B. die Email auf null ueberschrieben und der User kaeme nicht mehr rein.
     */
    @Test
    void updateUser_partialUpdate_onlyChangesProvidedFields() {
        testUser.setUserBio("original bio");
        testUser.setEmail("original@uzh.ch");
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UpdateUserPutDTO dto = new UpdateUserPutDTO();
        dto.setUserBio("new bio");

        userService.updateUser(1L, dto);

        assertEquals("new bio", testUser.getUserBio());
        assertEquals("original@uzh.ch", testUser.getEmail());
        assertEquals("secret123", testUser.getPassword());
        assertEquals("testUser", testUser.getUsername());
    }

    /**
     * Szenario: Update-Versuch auf nicht existierende User-ID.
     * Prueft: 404 NOT_FOUND wird geworfen.
     * Faengt Bug: Wenn .orElseThrow() zu .orElse(null) wird, gaebe es eine
     * NPE statt sauberer 404.
     */
    @Test
    void updateUser_unknownId_throwsNotFound() {
        Mockito.when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.updateUser(999L, new UpdateUserPutDTO()));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // --- logoutUser ---

    /**
     * Szenario: Eingeloggter User (ONLINE + Token) loggt sich aus.
     * Prueft: Token wird null, Status wird OFFLINE, save() wird aufgerufen.
     * Faengt Bug: Ungeloeschter Token koennte weiter fuer Requests genutzt
     * werden (Session-Fixation-Risiko). Fehlendes OFFLINE-Setzen zeigt User
     * weiterhin als online in der UI.
     */
    @Test
    void logoutUser_validUser_clearsTokenAndSetsOffline() {
        testUser.setStatus(UserStatus.ONLINE);
        testUser.setToken("active-token");
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        AuthHeader authHeader = Mockito.mock(AuthHeader.class);
        Mockito.when(authHeader.getUserId()).thenReturn(1L);

        userService.logoutUser(authHeader);

        assertNull(testUser.getToken());
        assertEquals(UserStatus.OFFLINE, testUser.getStatus());
        Mockito.verify(userRepository, Mockito.times(1)).save(testUser);
    }

    // --- getUsers ---

    /**
     * Szenario: Mehrere User existieren, getUsers() wird aufgerufen.
     * Prueft: Alle User aus dem Repository werden zurueckgegeben.
     * Faengt Bug: Sanity-Check gegen versehentlich eingebaute Filter (z.B.
     * "nur Non-Guests") die die Endpoint-Spec brechen wuerden.
     */
    @Test
    void getUsers_returnsAllUsersFromRepository() {
        User secondUser = new User();
        secondUser.setUserId(2L);
        secondUser.setUsername("user2");

        Mockito.when(userRepository.findAll()).thenReturn(List.of(testUser, secondUser));

        List<User> users = userService.getUsers();

        assertEquals(2, users.size());
        Mockito.verify(userRepository, Mockito.times(1)).findAll();
    }
}
