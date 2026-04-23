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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LobbyService.
 * All collaborators (UserService, GameService, SimpMessagingTemplate,
 * Repositories)
 * are mocked to isolate the LobbyService business logic.
 */
class LobbyServiceTest {

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

    @InjectMocks
    private LobbyService lobbyService;

    private User adminUser;
    private User secondUser;
    private CreateLobbyPostDTO createDTO;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        adminUser = new User();
        adminUser.setUserId(1L);
        adminUser.setUsername("admin");
        adminUser.setToken("admin-token");

        secondUser = new User();
        secondUser.setUserId(2L);
        secondUser.setUsername("player2");
        secondUser.setToken("token2");

        createDTO = new CreateLobbyPostDTO();
        createDTO.setLobbyName("TestLobby");
        createDTO.setSize(4);
        createDTO.setMaxRounds(5);
        createDTO.setVisibility(LobbyVisibility.PUBLIC);

        // Simulate the JPA behaviour of assigning an auto-generated ID on save().
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
     * Lobby.
     * Prüft: Die entsprechende Lobby wird aus activeLobbies entfernt und
     * getLobbyById(...) wirft danach 404.
     * Fängt Bug: Wenn @EventListener entfernt wird oder removeIf() den falschen
     * Vergleich macht, bleiben Zombie-Lobbies im Memory liegen.
     */
    @Test
    void onGameEnded_removesLobbyWithMatchingId() {
        Lobby lobby = setUpLobbyWithAdmin();
        Long lobbyId = lobby.getLobbyId();

        GameEndedEvent event = Mockito.mock(GameEndedEvent.class);
        Mockito.when(event.getGameId()).thenReturn(lobbyId);

        lobbyService.onGameEnded(event);

        assertThrows(ResponseStatusException.class,
                () -> lobbyService.getLobbyById(lobbyId));
    }

    // ═══════════════════════════════════════════════════════════════════
    // getAllLobbies
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: Zwei Lobbies wurden nacheinander erstellt.
     * Prüft: getAllLobbies() liefert beide zurück.
     * Fängt Bug: Wenn die interne Liste fälschlich bei jedem Aufruf geleert
     * oder nur das letzte Element zurückgegeben wird.
     */
    @Test
    void getAllLobbies_afterCreatingTwoLobbies_returnsBoth() {
        lobbyService.createLobby(createDTO, false, 1L, "admin-token");

        CreateLobbyPostDTO dto2 = new CreateLobbyPostDTO();
        dto2.setLobbyName("SecondLobby");
        dto2.setSize(4);
        dto2.setMaxRounds(5);
        dto2.setVisibility(LobbyVisibility.PUBLIC);
        lobbyService.createLobby(dto2, false, 2L, "token2");

        assertEquals(2, lobbyService.getAllLobbies().size());
    }

    // ═══════════════════════════════════════════════════════════════════
    // createLobby
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: Ein registrierter User erstellt eine Lobby (kein Guest).
     * Prüft: Lobby startet im Zustand WAITING, Admin ist korrekt gesetzt,
     * ein 4-stelliger Lobby-Code wurde generiert, noch keine User drin.
     * Fängt Bug: Falscher Initial-State (z. B. IN_GAME), vergessener Admin-Setup,
     * oder falsche Code-Länge (Off-by-One im createLobbyCode()).
     */
    @Test
    void createLobby_validInput_createsLobbyInWaitingStateWithGeneratedCode() {
        LobbyAccessDTO dto = lobbyService.createLobby(
                createDTO, false, 1L, "admin-token");

        Lobby lobby = lobbyService.getLobbyById(dto.getLobbyId());

        assertEquals(LobbyState.WAITING, lobby.getLobbyState());
        assertEquals(1L, lobby.getAdmin().getUserId());
        assertNotNull(lobby.getLobbyCode());
        // Business rule: the lobby code is a 4-character string (see CHARS / loop in
        // createLobbyCode)
        assertEquals(4, lobby.getLobbyCode().length());
        assertEquals(0, lobby.getUsers().size());
    }

    // ═══════════════════════════════════════════════════════════════════
    // joinLobby
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: Ein User joined mit korrektem Code in eine freie Lobby.
     * Prüft: Der User ist danach in der Lobby (existsUser == true).
     * Fängt Bug: Wenn addUser() vergessen wird oder falsche ID abgelegt wird.
     */
    @Test
    void joinLobby_validCodeAndSpaceAvailable_addsUserToLobby() {
        Lobby lobby = setUpLobbyWithAdmin();
        Mockito.when(userService.getUserById(2L)).thenReturn(secondUser);

        lobbyService.joinLobby(2L, "token2",
                lobby.getLobbyId(), lobby.getLobbyCode(), false);

        assertTrue(lobby.existsUser(2L));
    }

    /**
     * Szenario: Ein User versucht mit falschem Lobby-Code zu joinen.
     * Prüft: Es wird 403 FORBIDDEN geworfen UND der User wurde nicht hinzugefügt.
     * Fängt Bug: Der Check könnte die Exception werfen, aber VORHER schon
     * addUser() aufgerufen haben (Reihenfolge-Bug).
     */
    @Test
    void joinLobby_wrongCode_throwsForbidden() {
        Lobby lobby = setUpLobbyWithAdmin();
        Mockito.when(userService.getUserById(2L)).thenReturn(secondUser);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.joinLobby(2L, "token2",
                        lobby.getLobbyId(), "WRONG", false));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertFalse(lobby.existsUser(2L));
    }

    /**
     * Szenario: Die Lobby ist bereits voll (size erreicht), ein weiterer User will
     * rein.
     * Prüft: Es wird 409 CONFLICT geworfen und der neue User wurde nicht
     * hinzugefügt.
     * Fängt Bug: Klassischer Off-by-One: '>' statt '>=' beim Full-Check. Dieser
     * Test
     * würde failen, weil der dritte User noch durchgerutscht wäre.
     */
    @Test
    void joinLobby_lobbyFull_throwsConflict() {
        // Lobby mit size=1 -> nach einem Join ist sie voll
        createDTO.setSize(1);
        LobbyAccessDTO dto = lobbyService.createLobby(
                createDTO, false, 1L, "admin-token");
        Lobby lobby = lobbyService.getLobbyById(dto.getLobbyId());

        Mockito.when(userService.getUserById(2L)).thenReturn(secondUser);
        lobbyService.joinLobby(2L, "token2",
                lobby.getLobbyId(), lobby.getLobbyCode(), false);

        // Dritter User soll abgelehnt werden
        User third = new User();
        third.setUserId(3L);
        third.setUsername("player3");
        third.setToken("token3");
        Mockito.when(userService.getUserById(3L)).thenReturn(third);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.joinLobby(3L, "token3",
                        lobby.getLobbyId(), lobby.getLobbyCode(), false));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertFalse(lobby.existsUser(3L));
    }

    // ═══════════════════════════════════════════════════════════════════
    // startGame
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: Der Admin startet das Spiel in einer WAITING-Lobby.
     * Prüft: Lobby-State wechselt auf IN_GAME, das vom GameService
     * vorbereitete Game-Objekt wird auf der Lobby gesetzt,
     * gameService.setupGame(...) wurde genau einmal aufgerufen.
     * Fängt Bug: Falscher State nach Start (WAITING vergessen zu wechseln) oder
     * Game-Objekt wird nicht auf der Lobby gespeichert.
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
     * Szenario: Der Admin verlässt die Lobby, aber es ist noch mindestens
     * ein weiterer Spieler drin.
     * Prüft: Die Admin-Rolle wird auf den verbliebenen Spieler übertragen.
     * Fängt Bug: Ohne diesen Transfer hätte die Lobby keinen Admin mehr und
     * niemand könnte das Spiel starten (= tote Lobby).
     */
    @Test
    void leaveLobby_adminLeavesWithRemainingPlayers_transfersAdminRole() {
        Lobby lobby = setUpLobbyWithAdmin();
        Mockito.when(userService.getUserById(2L)).thenReturn(secondUser);
        lobbyService.joinLobby(2L, "token2",
                lobby.getLobbyId(), lobby.getLobbyCode(), false);

        lobbyService.leaveLobby(lobby.getLobbyId(), 1L);

        assertEquals(2L, lobby.getAdmin().getUserId());
    }

    /**
     * Szenario: Der letzte verbleibende User verlässt die Lobby.
     * Prüft: Die Lobby wird komplett entfernt (getLobbyById wirft 404).
     * Fängt Bug: Falls der Cleanup vergessen wird, sammeln sich leere Lobbies
     * im Memory und tauchen in getAllLobbies() auf.
     */
    @Test
    void leaveLobby_lastUserLeaves_removesLobbyFromActiveLobbies() {
        Lobby lobby = setUpLobbyWithAdmin();
        Long lobbyId = lobby.getLobbyId();

        lobbyService.leaveLobby(lobbyId, 1L);

        assertThrows(ResponseStatusException.class,
                () -> lobbyService.getLobbyById(lobbyId));
    }

    // ═══════════════════════════════════════════════════════════════════
    // getLobby (with membership check)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: Ein User, der NICHT in der Lobby ist, fragt die Lobby-Daten an.
     * Prüft: Es wird 403 FORBIDDEN geworfen.
     * Fängt Bug: Ohne diesen Check könnte jeder beliebige User fremde Lobby-
     * States einsehen — das wäre ein Privacy-Leak.
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
     * Prüft: Die Lobby wird ohne Fehler zurückgegeben.
     * Fängt Bug: Wenn die existsUser()-Prüfung invertiert wäre, würden
     * legitime User abgewiesen.
     */
    @Test
    void getLobby_userIsInLobby_returnsLobby() {
        Lobby lobby = setUpLobbyWithAdmin();
        Mockito.when(userService.getUserById(2L)).thenReturn(secondUser);
        lobbyService.joinLobby(2L, "token2",
                lobby.getLobbyId(), lobby.getLobbyCode(), false);

        Lobby result = lobbyService.getLobby(lobby.getLobbyId(), 2L);

        assertEquals(lobby.getLobbyId(), result.getLobbyId());
    }

    // ═══════════════════════════════════════════════════════════════════
    // getLobbyById
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: Es wird eine Lobby-ID abgefragt, die es nicht gibt.
     * Prüft: Es wird 404 NOT_FOUND geworfen.
     * Fängt Bug: Ein stillschweigendes 'return null' würde dazu führen,
     * dass Callers eine NPE bekommen statt einer sauberen 404.
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
     * Szenario: createGuestUser() wird aufgerufen (z. B. wenn ein anonymer
     * Besucher eine Lobby beitreten will).
     * Prüft: Der erzeugte User hat das isGuest-Flag gesetzt, hat einen Token
     * (weil am Ende loginUser() aufgerufen wird), und UserService
     * wurde sowohl für Registrierung als auch Login genau einmal
     * aufgerufen.
     * Fängt Bug: Wenn isGuest=false vergessen wird, erscheint der Gast in
     * öffentlichen User-Listen. Wenn loginUser() fehlt, hat der Gast
     * keinen Token und kann nicht auf geschützte Endpoints zugreifen.
     */
    @Test
    void createGuestUser_setsGuestFlagAndDelegatesToUserService() {
        User guest = new User();
        guest.setUserId(42L);
        guest.setUsername("guest_abc12345");
        guest.setPassword("dummy-password"); // ← NEU: damit loginUser() nicht null bekommt
        guest.setIsGuest(true);
        guest.setToken("guest-token");

        Mockito.when(userService.registerUser(Mockito.any(User.class))).thenReturn(guest);
        Mockito.when(userService.loginUser(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(guest);

        User result = lobbyService.createGuestUser();

        assertTrue(result.getIsGuest());
        assertNotNull(result.getToken());
        Mockito.verify(userService, Mockito.times(1)).registerUser(Mockito.any(User.class));
        Mockito.verify(userService, Mockito.times(1))
                .loginUser(Mockito.anyString(), Mockito.anyString());
    }
}