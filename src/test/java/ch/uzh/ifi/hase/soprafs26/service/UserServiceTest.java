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

    // Constants instead of magic numbers for readability
    private static final Long USER_ID = 1L;
    private static final Long UNKNOWN_ID = 999L;
    private static final String USERNAME = "testUser";
    private static final String EMAIL = "test@uzh.ch";
    private static final String PASSWORD = "secret123";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setUserId(USER_ID);
        testUser.setUsername(USERNAME);
        testUser.setEmail(EMAIL);
        testUser.setPassword(PASSWORD);

        // save(user) returns the same user, mimicking JPA behaviour.
        Mockito.when(userRepository.save(Mockito.any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // --- registerUser ---

    /**
     * Szenario: Neuer User mit gueltigen, einzigartigen Daten wird registriert.
     * Prueft: Status OFFLINE, creationDate gesetzt, isGuest=false, Scoreboard
     * initialisiert mit 0 Punkten, Token ist NOCH null (bewusste Design-
     * Entscheidung: Token kommt erst beim Login), save() und flush() genau
     * einmal aufgerufen.
     * Faengt Bug: Vergessenes setStatus/setCreationDate bricht Acceptance
     * Criteria aus S1. Fehlendes flush() laesst Daten bei Rollbacks verloren.
     * Der explizite Token-null-Check dokumentiert die Design-Entscheidung,
     * damit niemand aus Versehen die auskommentierte Token-Generation
     * in registerUser() wieder aktiviert.
     */
    @Test
    void registerUser_validInput_setsDefaultsAndPersists() {
        Mockito.when(userRepository.findByUsername(USERNAME)).thenReturn(null);
        Mockito.when(userRepository.findByEmail(EMAIL)).thenReturn(null);

        User created = userService.registerUser(testUser);

        assertEquals(UserStatus.OFFLINE, created.getStatus());
        assertNotNull(created.getCreationDate());
        assertFalse(created.getIsGuest());
        assertNotNull(created.getUserScoreboard());
        assertEquals(0, created.getUserScoreboard().getTotalPoints());
        assertNull(created.getToken(),
                "Token must NOT be assigned at registration — it is set at login");
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
        Mockito.when(userRepository.findByUsername(USERNAME)).thenReturn(testUser);
        Mockito.when(userRepository.findByEmail(Mockito.anyString())).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.registerUser(testUser));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    /**
     * Szenario: Email ist bereits vergeben, Username nicht.
     * Prueft: 400 BAD_REQUEST wird geworfen, save() wird NIE aufgerufen.
     * Faengt Bug: Deckt den dritten Branch von checkIfUserExists() ab —
     * ohne diesen Test wuerde ein Bug, der nur die Username-Pruefung ausfuehrt
     * und die Email-Pruefung ueberspringt, unentdeckt bleiben.
     */
    @Test
    void registerUser_duplicateEmail_throwsBadRequest() {
        Mockito.when(userRepository.findByUsername(Mockito.anyString())).thenReturn(null);
        Mockito.when(userRepository.findByEmail(EMAIL)).thenReturn(testUser);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.registerUser(testUser));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    /**
     * Szenario: SOWOHL Username als auch Email sind bereits vergeben.
     * Prueft: 400 BAD_REQUEST wird geworfen, save() wird NIE aufgerufen.
     * Faengt Bug: Erster Branch von checkIfUserExists() — deckt den
     * zusammengesetzten Fehlerfall ab. Wenn dieser Branch vertauscht wird,
     * kaeme eine ungenauere Fehlermeldung zum Client zurueck.
     */
    @Test
    void registerUser_bothUsernameAndEmailDuplicate_throwsBadRequest() {
        Mockito.when(userRepository.findByUsername(Mockito.anyString())).thenReturn(testUser);
        Mockito.when(userRepository.findByEmail(Mockito.anyString())).thenReturn(testUser);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.registerUser(testUser));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    // --- loginUser ---

    /**
     * Szenario: Registrierter User loggt sich mit korrektem Passwort ein.
     * Prueft: Status wechselt auf ONLINE, Token wird zugewiesen, save() und
     * flush() werden aufgerufen (damit der neue Status persistiert wird),
     * findByToken wird (mindestens einmal) aufgerufen zur Uniqueness-Pruefung.
     * Faengt Bug: Vergessenes save() wuerde den User nur in-memory ONLINE
     * setzen — beim naechsten AuthHeader-Check ist der User wieder OFFLINE,
     * Login waere de facto kaputt. Vergessenes setToken/setStatus bricht
     * die UI.
     */
    @Test
    void loginUser_validCredentials_setsStatusOnlineAndAssignsTokenAndPersists() {
        testUser.setStatus(UserStatus.OFFLINE);
        testUser.setToken(null);
        Mockito.when(userRepository.findByUsername(USERNAME)).thenReturn(testUser);
        Mockito.when(userRepository.findByToken(Mockito.anyString())).thenReturn(null);

        User logged = userService.loginUser(USERNAME, PASSWORD);

        assertEquals(UserStatus.ONLINE, logged.getStatus());
        assertNotNull(logged.getToken());
        Mockito.verify(userRepository, Mockito.times(1)).save(testUser);
        Mockito.verify(userRepository, Mockito.times(1)).flush();
        // Schleifen-Sanity: genau ein Token-Uniqueness-Check (Mock liefert null → 1
        // Iteration)
        Mockito.verify(userRepository, Mockito.times(1))
                .findByToken(Mockito.anyString());
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
        Mockito.when(userRepository.findByUsername(USERNAME)).thenReturn(testUser);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.loginUser(USERNAME, "WRONG_PASSWORD"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals(UserStatus.OFFLINE, testUser.getStatus());
        assertNull(testUser.getToken());
    }

    // --- getUserById ---

    /**
     * Szenario: User existiert und wird per ID abgefragt.
     * Prueft: Der richtige User wird zurueckgegeben (Happy-Path-Partner
     * zum Error-Test unten).
     * Faengt Bug: Wenn jemand getUserById() so umschreibt, dass es immer
     * 404 wirft (z. B. Bedingung invertiert), bleibt der Error-Test gruen —
     * aber die Funktionalitaet ist tot. Dieser Test deckt genau das auf.
     */
    @Test
    void getUserById_existingUser_returnsUser() {
        Mockito.when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        User result = userService.getUserById(USER_ID);

        assertEquals(testUser.getUserId(), result.getUserId());
        assertEquals(testUser.getUsername(), result.getUsername());
    }

    /**
     * Szenario: Abfrage einer User-ID, die nicht existiert.
     * Prueft: 404 NOT_FOUND wird geworfen.
     * Faengt Bug: Assignment verlangt in User Story S2 explizit 404. Ein
     * stillschweigendes 'return null' wuerde die REST-Spec brechen.
     */
    @Test
    void getUserById_unknownId_throwsNotFound() {
        Mockito.when(userRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.getUserById(UNKNOWN_ID));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // --- updateUser ---

    /**
     * Szenario: User aktualisiert NUR die Bio, andere Felder bleiben null im DTO.
     * Prueft: Bio wurde aktualisiert, Email/Password/Username bleiben erhalten,
     * save() wird aufgerufen (damit die Aenderung persistiert wird).
     * Faengt Bug: Klassischer HTTP-PUT-Bug. Das Assignment verlangt explizit,
     * dass nur uebergebene Felder aktualisiert werden. Ohne null-Checks wuerde
     * z.B. die Email auf null ueberschrieben. Fehlendes save() wuerde die
     * Aenderung in-memory machen aber nicht persistieren.
     */
    @Test
    void updateUser_partialUpdate_onlyChangesProvidedFieldsAndPersists() {
        testUser.setUserBio("original bio");
        testUser.setEmail("original@uzh.ch");
        Mockito.when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        UpdateUserPutDTO dto = new UpdateUserPutDTO();
        dto.setUserBio("new bio");

        userService.updateUser(USER_ID, dto);

        assertEquals("new bio", testUser.getUserBio());
        assertEquals("original@uzh.ch", testUser.getEmail());
        assertEquals(PASSWORD, testUser.getPassword());
        assertEquals(USERNAME, testUser.getUsername());
        Mockito.verify(userRepository, Mockito.times(1)).save(testUser);
    }

    /**
     * Szenario: Update-Versuch auf nicht existierende User-ID.
     * Prueft: 404 NOT_FOUND wird geworfen.
     * Faengt Bug: Wenn .orElseThrow() zu .orElse(null) wird, gaebe es eine
     * NPE statt sauberer 404.
     */
    @Test
    void updateUser_unknownId_throwsNotFound() {
        Mockito.when(userRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.updateUser(UNKNOWN_ID, new UpdateUserPutDTO()));

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
        Mockito.when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        AuthHeader authHeader = Mockito.mock(AuthHeader.class);
        Mockito.when(authHeader.getUserId()).thenReturn(USER_ID);

        userService.logoutUser(authHeader);

        assertNull(testUser.getToken());
        assertEquals(UserStatus.OFFLINE, testUser.getStatus());
        Mockito.verify(userRepository, Mockito.times(1)).save(testUser);
    }

    /**
     * Szenario: Logout-Versuch fuer einen User, dessen ID nicht existiert.
     * Prueft: Der Service wirft aktuell eine NullPointerException.
     * Faengt Bug: DOKUMENTIERT einen bekannten Service-Bug.
     * UserService.logoutUser() nutzt findById(...).orElse(null) ohne
     * null-Check. Ein ungueltiger AuthHeader fuehrt deshalb zu einer NPE
     * statt einer sauberen 404.
     * TODO: Sobald der Service auf .orElseThrow() umgestellt ist,
     * muss dieser Test auf ResponseStatusException mit Status 404
     * geaendert werden.
     */
    @Test
    void logoutUser_unknownUser_currentlyThrowsNPE() {
        Mockito.when(userRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        AuthHeader authHeader = Mockito.mock(AuthHeader.class);
        Mockito.when(authHeader.getUserId()).thenReturn(UNKNOWN_ID);

        assertThrows(NullPointerException.class,
                () -> userService.logoutUser(authHeader));
    }

    // --- getUsers ---

    /**
     * Szenario: Mehrere User existieren, getUsers() wird aufgerufen.
     * Prueft: Alle User aus dem Repository werden zurueckgegeben.
     * Faengt Bug: Sanity-Check gegen versehentlich eingebaute Filter (z.B.
     * "nur Non-Guests") die die Endpoint-Spec brechen wuerden.
     * Hinweis: Dieser Test hat den geringsten Bug-catching-Value der Suite,
     * weil getUsers() ein Einzeiler-Delegation an findAll() ist.
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