package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;
import ch.uzh.ifi.hase.soprafs26.entity.GameResult;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.events.GameEndedEvent;
import ch.uzh.ifi.hase.soprafs26.objects.Game;
import ch.uzh.ifi.hase.soprafs26.objects.Lobby;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CreateLobbyPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyAccessDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LobbyService.
 * All collaborators (UserService, GameService, SimpMessagingTemplate,
 * Repositories) are mocked to isolate the LobbyService business logic.
 *
 * The LobbyService under test is explicitly re-instantiated in @BeforeEach
 * because it holds activeLobbies as an instance field (in-memory state).
 * Without a fresh service per test, tests could bleed state into each other
 * and produce non-deterministic results.
 */
class LobbyServiceTest {

    // Constants instead of magic numbers for better readability
    private static final Long ADMIN_ID = 1L;
    private static final Long SECOND_USER_ID = 2L;
    private static final Long THIRD_USER_ID = 3L;
    private static final String ADMIN_TOKEN = "admin-token";
    private static final String SECOND_TOKEN = "token2";

    @Mock
    private UserService userService;
    @Mock
    private GameService gameService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GameRepository gameRepository;

    // No @InjectMocks — we build the service manually in setup()
    // to guarantee a fresh instance per test (fixes shared-state bug).
    private LobbyService lobbyService;

    private User adminUser;
    private User secondUser;
    private CreateLobbyPostDTO createDTO;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        // Explicitly rebuild the service per test — activeLobbies starts empty.
        lobbyService = new LobbyService(
                userService, gameService, messagingTemplate, userRepository, gameRepository);

        adminUser = new User();
        adminUser.setUserId(ADMIN_ID);
        adminUser.setUsername("admin");
        adminUser.setToken(ADMIN_TOKEN);

        secondUser = new User();
        secondUser.setUserId(SECOND_USER_ID);
        secondUser.setUsername("player2");
        secondUser.setToken(SECOND_TOKEN);

        createDTO = new CreateLobbyPostDTO();
        createDTO.setLobbyName("TestLobby");
        createDTO.setSize(4);
        createDTO.setMaxRounds(5);
        createDTO.setVisibility(LobbyVisibility.PUBLIC);

        // Simulate JPA behaviour of assigning an auto-generated ID on save().
        // Without this, createLobby() would call gameResult.getGameId() and get null.
        AtomicLong gameIdCounter = new AtomicLong(100);
        Mockito.doAnswer(inv -> {
            GameResult gr = inv.getArgument(0);
            gr.setGameId(gameIdCounter.incrementAndGet());
            return gr;
        }).when(gameRepository).save(Mockito.any(GameResult.class));
    }

    /**
     * Helper: creates a fresh lobby with adminUser as admin and no other players.
     * Used by every test that needs a pre-existing lobby to act on.
     */
    private Lobby setUpLobbyWithAdmin() {
        LobbyAccessDTO dto = lobbyService.createLobby(
                createDTO, false, adminUser.getUserId(), adminUser.getToken());
        return lobbyService.getLobbyById(dto.getLobbyId());
    }

    // ═══════════════════════════════════════════════════════════════════
    // onGameEnded
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: Spring publiziert ein GameEndedEvent mit der ID einer aktiven
     * Lobby, während eine zweite, nicht betroffene Lobby ebenfalls aktiv ist.
     * Prueft: Nur die Lobby mit passender ID wird entfernt, die andere bleibt.
     * Faengt Bug: Ein zu aggressives removeIf() würde alle Lobbies löschen.
     * Ein fehlender Vergleich würde die falsche Lobby entfernen.
     */
    @Test
    void onGameEnded_removesOnlyMatchingLobby() {
        Lobby lobbyA = setUpLobbyWithAdmin();

        // Zweite Lobby mit anderem User als Admin erstellen
        LobbyAccessDTO dtoB = lobbyService.createLobby(
                createDTO, false, SECOND_USER_ID, SECOND_TOKEN);
        Long lobbyBId = dtoB.getLobbyId();

        GameEndedEvent event = Mockito.mock(GameEndedEvent.class);
        Mockito.when(event.getGameId()).thenReturn(lobbyA.getLobbyId());

        lobbyService.onGameEnded(event);

        // Lobby A ist weg
        assertThrows(ResponseStatusException.class,
                () -> lobbyService.getLobbyById(lobbyA.getLobbyId()));
        // Lobby B überlebt
        assertNotNull(lobbyService.getLobbyById(lobbyBId));
    }

    // ═══════════════════════════════════════════════════════════════════
    // getAllLobbies
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: Zwei Lobbies wurden nacheinander erstellt.
     * Prueft: getAllLobbies() liefert genau beide zurück.
     * Faengt Bug: Wenn die interne Liste fälschlich bei jedem Aufruf geleert
     * oder nur das letzte Element zurückgegeben wird.
     * Hinweis: Dieser Test funktioniert nur deterministisch dank frischem
     * LobbyService pro Test (siehe @BeforeEach).
     */
    @Test
    void getAllLobbies_afterCreatingTwoLobbies_returnsBoth() {
        lobbyService.createLobby(createDTO, false, ADMIN_ID, ADMIN_TOKEN);

        CreateLobbyPostDTO dto2 = new CreateLobbyPostDTO();
        dto2.setLobbyName("SecondLobby");
        dto2.setSize(4);
        dto2.setMaxRounds(5);
        dto2.setVisibility(LobbyVisibility.PUBLIC);
        lobbyService.createLobby(dto2, false, SECOND_USER_ID, SECOND_TOKEN);

        assertEquals(2, lobbyService.getAllLobbies().size());
    }

    // ═══════════════════════════════════════════════════════════════════
    // createLobby
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: Ein registrierter User erstellt eine Lobby (kein Guest).
     * Prueft: Lobby startet im Zustand WAITING, Admin ist korrekt gesetzt,
     * ein 4-stelliger Lobby-Code wurde generiert.
     * Faengt Bug: Falscher Initial-State (z. B. IN_GAME), vergessener
     * Admin-Setup oder falsche Code-Länge (Off-by-One im createLobbyCode()).
     */
    @Test
    void createLobby_validInput_createsLobbyInWaitingStateWithGeneratedCode() {
        LobbyAccessDTO dto = lobbyService.createLobby(
                createDTO, false, ADMIN_ID, ADMIN_TOKEN);

        Lobby lobby = lobbyService.getLobbyById(dto.getLobbyId());

        assertEquals(LobbyState.WAITING, lobby.getLobbyState());
        assertEquals(ADMIN_ID, lobby.getAdmin().getUserId());
        assertNotNull(lobby.getLobbyCode());
        // Business rule: lobby code is always 4 characters (see CHARS loop)
        assertEquals(4, lobby.getLobbyCode().length());
    }

    /**
     * Szenario: Ein Admin fragt seine eigene Lobby über getLobby() ab,
     * direkt nach dem Erstellen — OHNE vorher zu "joinen".
     * Prueft: Aktueller Stand dokumentiert: Es wird 403 FORBIDDEN geworfen.
     * Grund: createLobby() fügt den Admin NICHT in die users-Map ein,
     * also schlägt existsUser(adminId) fehl.
     * TODO: Dies ist ein DESIGN-BUG im LobbyService. Sobald createLobby()
     * fixed ist (Admin wird in users-Map aufgenommen), muss dieser Test
     * auf assertDoesNotThrow und assertEquals(lobbyId, ...) geändert werden.
     */
    @Test
    void getLobby_adminRequestsOwnLobby_currentlyFailsWithForbidden_designBug() {
        Lobby lobby = setUpLobbyWithAdmin();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.getLobby(lobby.getLobbyId(), ADMIN_ID));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ═══════════════════════════════════════════════════════════════════
    // joinLobby
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: Ein User joined mit korrektem Code in eine freie Lobby.
     * Prueft: Der User ist danach in der Lobby UND das zurückgegebene
     * LobbyAccessDTO enthält die korrekte userId, token und lobbyId.
     * Faengt Bug: Wenn addUser() vergessen wird ODER das DTO falsch gefüllt
     * wird (z. B. fremde userId eingetragen → Security-Leak).
     */
    @Test
    void joinLobby_validCodeAndSpaceAvailable_addsUserAndReturnsCorrectDTO() {
        Lobby lobby = setUpLobbyWithAdmin();
        Mockito.when(userService.getUserById(SECOND_USER_ID)).thenReturn(secondUser);

        LobbyAccessDTO result = lobbyService.joinLobby(
                SECOND_USER_ID, SECOND_TOKEN,
                lobby.getLobbyId(), lobby.getLobbyCode(), false);

        // Side-Effect: User ist in der Lobby
        assertTrue(lobby.existsUser(SECOND_USER_ID));

        // Return-Value: DTO ist korrekt ausgefüllt
        assertEquals(SECOND_USER_ID, result.getUserId());
        assertEquals(SECOND_TOKEN, result.getToken());
        assertEquals(lobby.getLobbyId(), result.getLobbyId());
        assertEquals(lobby.getLobbyCode(), result.getLobbyCode());
    }

    /**
     * Szenario: Ein User versucht mit falschem Lobby-Code zu joinen.
     * Prueft: Es wird 403 FORBIDDEN geworfen UND der User wurde nicht hinzugefügt.
     * Faengt Bug: Der Check könnte die Exception werfen, aber VORHER schon
     * addUser() aufgerufen haben (Reihenfolge-Bug).
     */
    @Test
    void joinLobby_wrongCode_throwsForbidden() {
        Lobby lobby = setUpLobbyWithAdmin();
        Mockito.when(userService.getUserById(SECOND_USER_ID)).thenReturn(secondUser);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.joinLobby(SECOND_USER_ID, SECOND_TOKEN,
                        lobby.getLobbyId(), "WRONG", false));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertFalse(lobby.existsUser(SECOND_USER_ID));
    }

    /**
     * Szenario: Die Lobby ist bereits voll (size erreicht), ein weiterer User
     * will rein.
     * Prueft: Es wird 409 CONFLICT geworfen und der neue User wurde nicht
     * hinzugefügt.
     * Faengt Bug: Klassischer Off-by-One: '>' statt '>=' beim Full-Check.
     */
    @Test
    void joinLobby_lobbyFull_throwsConflict() {
        createDTO.setSize(1);
        LobbyAccessDTO dto = lobbyService.createLobby(
                createDTO, false, ADMIN_ID, ADMIN_TOKEN);
        Lobby lobby = lobbyService.getLobbyById(dto.getLobbyId());

        Mockito.when(userService.getUserById(SECOND_USER_ID)).thenReturn(secondUser);
        lobbyService.joinLobby(SECOND_USER_ID, SECOND_TOKEN,
                lobby.getLobbyId(), lobby.getLobbyCode(), false);

        // Dritter User soll abgelehnt werden
        User third = new User();
        third.setUserId(THIRD_USER_ID);
        third.setUsername("player3");
        third.setToken("token3");
        Mockito.when(userService.getUserById(THIRD_USER_ID)).thenReturn(third);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.joinLobby(THIRD_USER_ID, "token3",
                        lobby.getLobbyId(), lobby.getLobbyCode(), false));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertFalse(lobby.existsUser(THIRD_USER_ID));
    }

    // ═══════════════════════════════════════════════════════════════════
    // startGame
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: Der Admin startet das Spiel in einer WAITING-Lobby.
     * Prueft: Lobby-State wechselt auf IN_GAME, das vom GameService
     * vorbereitete Game-Objekt wird auf der Lobby gesetzt,
     * gameService.setupGame(...) wurde genau einmal aufgerufen.
     * Faengt Bug: Falscher State nach Start oder Game-Objekt wird nicht
     * auf der Lobby gespeichert.
     */
    @Test
    void startGame_validLobby_transitionsStateToInGame() {
        Lobby lobby = setUpLobbyWithAdmin();

        Game mockedGame = Mockito.mock(Game.class);
        Mockito.when(gameService.setupGame(lobby)).thenReturn(mockedGame);

        lobbyService.startGame(lobby.getLobbyId());

        assertEquals(LobbyState.IN_GAME, lobby.getLobbyState());
        assertEquals(mockedGame, lobby.getGame());
        Mockito.verify(gameService, Mockito.times(1)).setupGame(lobby);
    }

    // ═══════════════════════════════════════════════════════════════════
    // leaveLobby
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: Der Admin verlässt eine Lobby mit mehreren weiteren Spielern.
     * Prueft: Die Admin-Rolle wird auf EINEN der verbliebenen Spieler
     * übertragen — wir prüfen nicht auf eine spezifische ID, weil die
     * HashMap-Reihenfolge nicht deterministisch ist.
     * Faengt Bug: Ohne Transfer hätte die Lobby keinen Admin mehr.
     * Dokumentiert gleichzeitig, dass die Admin-Auswahl aktuell
     * nicht deterministisch ist (getUsers().get(0) auf HashMap.values()).
     */
    @Test
    void leaveLobby_adminLeavesWithMultiplePlayers_transfersAdminToOneOfRemaining() {
        Lobby lobby = setUpLobbyWithAdmin();

        // Zwei weitere Spieler beitreten lassen
        Mockito.when(userService.getUserById(SECOND_USER_ID)).thenReturn(secondUser);
        lobbyService.joinLobby(SECOND_USER_ID, SECOND_TOKEN,
                lobby.getLobbyId(), lobby.getLobbyCode(), false);

        User third = new User();
        third.setUserId(THIRD_USER_ID);
        third.setUsername("player3");
        third.setToken("token3");
        Mockito.when(userService.getUserById(THIRD_USER_ID)).thenReturn(third);
        lobbyService.joinLobby(THIRD_USER_ID, "token3",
                lobby.getLobbyId(), lobby.getLobbyCode(), false);

        // Admin verlässt
        lobbyService.leaveLobby(lobby.getLobbyId(), ADMIN_ID);

        // Neuer Admin muss einer der verbliebenen sein — nicht der Ex-Admin
        Long newAdminId = lobby.getAdmin().getUserId();
        assertNotEquals(ADMIN_ID, newAdminId, "Admin role must not stay with the user who left");
        assertTrue(Set.of(SECOND_USER_ID, THIRD_USER_ID).contains(newAdminId),
                "New admin must be one of the remaining players");
    }

    /**
     * Szenario: Der letzte verbleibende User verlässt die Lobby.
     * Prueft: Die Lobby wird komplett entfernt (getLobbyById wirft 404).
     * Faengt Bug: Falls der Cleanup vergessen wird, sammeln sich leere
     * Lobbies im Memory und tauchen in getAllLobbies() auf.
     */
    @Test
    void leaveLobby_lastUserLeaves_removesLobbyFromActiveLobbies() {
        Lobby lobby = setUpLobbyWithAdmin();
        Long lobbyId = lobby.getLobbyId();

        lobbyService.leaveLobby(lobbyId, ADMIN_ID);

        assertThrows(ResponseStatusException.class,
                () -> lobbyService.getLobbyById(lobbyId));
    }

    // ═══════════════════════════════════════════════════════════════════
    // getLobby (with membership check)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: Ein User, der NICHT in der Lobby ist, fragt die Lobby-Daten an.
     * Prueft: Es wird 403 FORBIDDEN geworfen.
     * Faengt Bug: Ohne diesen Check könnte jeder User fremde Lobby-States
     * einsehen — ein Privacy-Leak.
     */
    @Test
    void getLobby_userNotInLobby_throwsForbidden() {
        Lobby lobby = setUpLobbyWithAdmin();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.getLobby(lobby.getLobbyId(), 999L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    /**
     * Szenario: Ein User, der Mitglied der Lobby ist, fragt die Lobby-Daten an.
     * Prueft: Die Lobby wird ohne Fehler zurückgegeben.
     * Faengt Bug: Wenn die existsUser()-Prüfung invertiert wäre, würden
     * legitime User abgewiesen.
     */
    @Test
    void getLobby_userIsInLobby_returnsLobby() {
        Lobby lobby = setUpLobbyWithAdmin();
        Mockito.when(userService.getUserById(SECOND_USER_ID)).thenReturn(secondUser);
        lobbyService.joinLobby(SECOND_USER_ID, SECOND_TOKEN,
                lobby.getLobbyId(), lobby.getLobbyCode(), false);

        Lobby result = lobbyService.getLobby(lobby.getLobbyId(), SECOND_USER_ID);

        assertEquals(lobby.getLobbyId(), result.getLobbyId());
    }

    // ═══════════════════════════════════════════════════════════════════
    // getLobbyById
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: Es wird eine Lobby-ID abgefragt, die es nicht gibt.
     * Prueft: Es wird 404 NOT_FOUND geworfen.
     * Faengt Bug: Ein stillschweigendes 'return null' würde NPEs beim
     * Caller auslösen statt einer sauberen 404.
     */
    @Test
    void getLobbyById_unknownId_throwsNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.getLobbyById(9999L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ═══════════════════════════════════════════════════════════════════
    // createGuestUser
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: createGuestUser() wird aufgerufen (anonymer Lobby-Beitritt).
     * Prueft: Der User, der an registerUser() übergeben wird, hat das
     * isGuest-Flag gesetzt UND einen Username mit "guest_"-Prefix.
     * Danach wird loginUser() genau einmal aufgerufen.
     * Faengt Bug: Ein naiver Test hätte nur den Mock-Return geprüft
     * (Tautologie). Der ArgumentCaptor prüft das ECHTE Verhalten: wenn
     * jemand setIsGuest(true) aus createGuestUser() entfernt, wird der
     * Test rot.
     */
    @Test
    void createGuestUser_setsGuestFlagAndDelegatesToUserService() {
        // Setup: registerUser und loginUser geben einen voll ausgestatteten Guest
        // zurück
        User guest = new User();
        guest.setUserId(42L);
        guest.setUsername("guest_abc12345");
        guest.setPassword("dummy-password");
        guest.setIsGuest(true);
        guest.setToken("guest-token");

        Mockito.when(userService.registerUser(Mockito.any(User.class))).thenReturn(guest);
        Mockito.when(userService.loginUser(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(guest);

        // Act
        User result = lobbyService.createGuestUser();

        // Assert 1: Das Endresultat hat Token (loginUser wurde aufgerufen)
        assertNotNull(result.getToken());

        // Assert 2: Was wurde TATSÄCHLICH an registerUser übergeben?
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        Mockito.verify(userService).registerUser(captor.capture());
        User capturedGuest = captor.getValue();

        assertTrue(capturedGuest.getIsGuest(),
                "createGuestUser must pass a User with isGuest=true to registerUser");
        assertTrue(capturedGuest.getUsername().startsWith("guest_"),
                "Guest username must start with 'guest_' prefix");

        // Assert 3: loginUser wurde genau einmal aufgerufen
        Mockito.verify(userService, Mockito.times(1))
                .loginUser(Mockito.anyString(), Mockito.anyString());
    }
}
