package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;
import ch.uzh.ifi.hase.soprafs26.constant.MessageType;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.RoundHistoryRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CreateLobbyPostDTO;
import ch.uzh.ifi.hase.soprafs26.websocket.Message;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LobbyService.
 *
 * All collaborators are mocked to isolate business logic.
 * Lobby is now a JPA entity persisted via LobbyRepository (no more in-memory
 * activeLobbies list). Tests therefore stub lobbyRepository rather than
 * relying on shared in-memory state. A fresh LobbyService is built manually
 * in @BeforeEach to keep the mock-setup deterministic.
 *
 * Removed tests (concept gone from the new design):
 * - onGameEnded: GameEndedEvent and the event-listener method no longer exist.
 */
class LobbyServiceTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long SECOND_USER_ID = 2L;
    private static final Long THIRD_USER_ID = 3L;
    private static final Long LOBBY_ID = 10L;
    private static final String ADMIN_TOKEN = "admin-token";
    private static final String SECOND_TOKEN = "token2";
    private static final String LOBBY_CODE = "ABCD";

    @Mock private UserService userService;
    @Mock private GameService gameService;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private UserRepository userRepository;
    @Mock private LobbyRepository lobbyRepository;
    @Mock private RoundHistoryRepository roundHistoryRepository;

    private LobbyService lobbyService;

    private User adminUser;
    private User secondUser;
    private CreateLobbyPostDTO createDTO;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        lobbyService = new LobbyService(
                userService, gameService, messagingTemplate,
                userRepository, lobbyRepository, roundHistoryRepository);

        adminUser = buildUser(ADMIN_ID, "admin", ADMIN_TOKEN);
        secondUser = buildUser(SECOND_USER_ID, "player2", SECOND_TOKEN);

        createDTO = new CreateLobbyPostDTO();
        createDTO.setLobbyName("TestLobby");
        createDTO.setMaxPlayers(4);
        createDTO.setMaxRounds(5);
        createDTO.setVisibility(LobbyVisibility.PUBLIC);

        // Default: lobby code is always unique
        Mockito.when(lobbyRepository.existsByLobbyCode(Mockito.anyString())).thenReturn(false);

        // Default: save() assigns a generated ID
        Mockito.when(lobbyRepository.save(Mockito.any(Lobby.class))).thenAnswer(inv -> {
            Lobby l = inv.getArgument(0);
            if (l.getLobbyId() == null) l.setLobbyId(LOBBY_ID);
            return l;
        });
    }

    // ═══════════════════════════════════════════════════════════════════
    // getAllLobbies
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: Die Lobby-Liste enthaelt WAITING- und FINISHED-Lobbies.
     * Prueft: getAllLobbies() filtert FINISHED heraus und gibt nur aktive zurueck.
     * Faengt Bug: Ohne den filter-Stream wuerde eine beendete Lobby sichtbar
     * bleiben und Clients mit einem falschen Zustand beliefern.
     */
    @Test
    void getAllLobbies_filtersOutFinishedLobbies() {
        Lobby waiting = buildLobby(1L, LobbyState.WAITING);
        Lobby inGame = buildLobby(2L, LobbyState.IN_GAME);
        Lobby finished = buildLobby(3L, LobbyState.FINISHED);
        Mockito.when(lobbyRepository.findAll()).thenReturn(List.of(waiting, inGame, finished));

        List<Lobby> result = lobbyService.getAllLobbies();

        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(l -> l.getLobbyState() == LobbyState.FINISHED));
    }

    /**
     * Szenario: Keine Lobbies vorhanden.
     * Prueft: Eine leere Liste wird zurueckgegeben (kein NPE, kein 404).
     */
    @Test
    void getAllLobbies_empty_returnsEmptyList() {
        Mockito.when(lobbyRepository.findAll()).thenReturn(List.of());

        List<Lobby> result = lobbyService.getAllLobbies();

        assertTrue(result.isEmpty());
    }

    // ═══════════════════════════════════════════════════════════════════
    // createLobby
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: Ein registrierter User erstellt eine Lobby.
     * Prueft: Die persistierte Lobby hat State WAITING, korrekte Admin-ID,
     * einen 4-stelligen Code; das zurueckgegebene DTO enthaelt lobbyId und token.
     * Faengt Bug: Falscher Initial-State, vergessener Admin-Setup oder falsche
     * Code-Laenge (Off-by-One im createLobbyCode()).
     */
    @Test
    void createLobby_validInput_persistsLobbyInWaitingStateWithCode() {
        Mockito.when(userService.getUserById(ADMIN_ID)).thenReturn(adminUser);

        LobbyAccessDTO dto = lobbyService.createLobby(createDTO, false, ADMIN_ID, ADMIN_TOKEN);

        ArgumentCaptor<Lobby> captor = ArgumentCaptor.forClass(Lobby.class);
        Mockito.verify(lobbyRepository).save(captor.capture());
        Lobby saved = captor.getValue();

        assertEquals(LobbyState.WAITING, saved.getLobbyState());
        assertEquals(ADMIN_ID, saved.getAdmin().getUserId());
        assertNotNull(saved.getLobbyCode());
        assertEquals(4, saved.getLobbyCode().length(),
                "Lobby code must always be exactly 4 characters");
        assertEquals(ADMIN_TOKEN, dto.getToken());
        assertEquals(LOBBY_ID, dto.getLobbyId());
    }

    /**
     * Szenario: Guest-User erstellt eine Lobby (isGuest=true, userId/token null).
     * Prueft: userService.createGuestUser() wird aufgerufen, die Lobby-IDs im DTO
     * gehoeren dem Guest.
     * Faengt Bug: Wenn der Guest-Branch den userId-Parameter statt der Guest-ID
     * ins DTO schreibt, wuerde ein Security-Leak entstehen.
     */
    @Test
    void createLobby_guestUser_delegatesToCreateGuestUser() {
        User guestUser = buildUser(99L, "guest_abc", "guest-token");
        Mockito.when(userService.createGuestUser()).thenReturn(guestUser);
        Mockito.when(userService.getUserById(99L)).thenReturn(guestUser);

        LobbyAccessDTO dto = lobbyService.createLobby(createDTO, true, null, null);

        Mockito.verify(userService).createGuestUser();
        assertEquals(99L, dto.getUserId());
        assertEquals("guest-token", dto.getToken());
    }

    // ═══════════════════════════════════════════════════════════════════
    // joinLobby
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: Ein User joined mit korrektem Code in eine freie Lobby.
     * Prueft: Der User wurde zur players-Liste hinzugefuegt, das zurueckgegebene
     * DTO enthaelt korrekte userId/token/lobbyId/lobbyCode.
     * Faengt Bug: Wenn addUser vergessen wird oder das DTO falsch befuellt wird
     * (fremde userId → Security-Leak).
     */
    @Test
    void joinLobby_validCodeAndSpaceAvailable_addsUserAndReturnsCorrectDTO() {
        Lobby lobby = buildLobby(LOBBY_ID, LobbyState.WAITING);
        lobby.setAdmin(adminUser);
        lobby.setLobbyCode(LOBBY_CODE);
        Mockito.when(lobbyRepository.findById(LOBBY_ID)).thenReturn(Optional.of(lobby));
        Mockito.when(userService.getUserById(SECOND_USER_ID)).thenReturn(secondUser);

        LobbyAccessDTO result = lobbyService.joinLobby(
                SECOND_USER_ID, SECOND_TOKEN, LOBBY_ID, LOBBY_CODE, false);

        assertTrue(lobby.getPlayers().stream()
                .anyMatch(p -> p.getUserId().equals(SECOND_USER_ID)));
        assertEquals(SECOND_USER_ID, result.getUserId());
        assertEquals(SECOND_TOKEN, result.getToken());
        assertEquals(LOBBY_ID, result.getLobbyId());
        assertEquals(LOBBY_CODE, result.getLobbyCode());
    }

    /**
     * Szenario: User versucht mit falschem Lobby-Code zu joinen.
     * Prueft: 403 FORBIDDEN, User wurde NICHT in die players-Liste aufgenommen.
     * Faengt Bug: Reihenfolge-Bug — wenn addUser vor dem Code-Check laeuft,
     * waere der User schon drin, obwohl er abgewiesen wird.
     */
    @Test
    void joinLobby_wrongCode_throwsForbidden() {
        Lobby lobby = buildLobby(LOBBY_ID, LobbyState.WAITING);
        lobby.setAdmin(adminUser);
        lobby.setLobbyCode(LOBBY_CODE);
        Mockito.when(lobbyRepository.findById(LOBBY_ID)).thenReturn(Optional.of(lobby));
        Mockito.when(userService.getUserById(SECOND_USER_ID)).thenReturn(secondUser);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.joinLobby(SECOND_USER_ID, SECOND_TOKEN, LOBBY_ID, "WRONG", false));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertFalse(lobby.getPlayers().stream()
                .anyMatch(p -> p.getUserId().equals(SECOND_USER_ID)));
    }

    /**
     * Szenario: Die Lobby ist voll (players.size() >= maxPlayers), weiterer User will rein.
     * Prueft: 409 CONFLICT, neuer User wurde nicht hinzugefuegt.
     * Faengt Bug: Klassischer Off-by-One: '>' statt '>=' beim Full-Check.
     */
    @Test
    void joinLobby_lobbyFull_throwsConflict() {
        User player1 = buildUser(SECOND_USER_ID, "p1", SECOND_TOKEN);
        Lobby lobby = buildLobby(LOBBY_ID, LobbyState.WAITING);
        lobby.setAdmin(adminUser);
        lobby.setLobbyCode(LOBBY_CODE);
        lobby.setMaxPlayers(1);
        lobby.getPlayers().add(player1);
        Mockito.when(lobbyRepository.findById(LOBBY_ID)).thenReturn(Optional.of(lobby));

        User third = buildUser(THIRD_USER_ID, "p3", "token3");
        Mockito.when(userService.getUserById(THIRD_USER_ID)).thenReturn(third);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.joinLobby(THIRD_USER_ID, "token3", LOBBY_ID, LOBBY_CODE, false));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertFalse(lobby.getPlayers().stream()
                .anyMatch(p -> p.getUserId().equals(THIRD_USER_ID)));
    }

    /**
     * Szenario: User ist bereits in der Lobby und ruft joinLobby erneut auf.
     * Prueft: Idempotente Rueckgabe — kein zweites Einfuegen, DTO ist korrekt.
     * Faengt Bug: Doppeltes Hinzufuegen zur players-Liste wuerde die Lobby-Groesse
     * verfaelschen und die Vollstaendigkeitspruefung kaputt machen.
     */
    @Test
    void joinLobby_userAlreadyInLobby_returnsExistingDTO() {
        Lobby lobby = buildLobby(LOBBY_ID, LobbyState.WAITING);
        lobby.setAdmin(adminUser);
        lobby.setLobbyCode(LOBBY_CODE);
        lobby.getPlayers().add(secondUser);
        Mockito.when(lobbyRepository.findById(LOBBY_ID)).thenReturn(Optional.of(lobby));
        Mockito.when(userService.getUserById(SECOND_USER_ID)).thenReturn(secondUser);

        LobbyAccessDTO result = lobbyService.joinLobby(
                SECOND_USER_ID, SECOND_TOKEN, LOBBY_ID, LOBBY_CODE, false);

        assertEquals(1, lobby.getPlayers().size(), "Should not be added twice");
        assertEquals(SECOND_USER_ID, result.getUserId());
    }

    // ═══════════════════════════════════════════════════════════════════
    // startGame
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: Admin startet das Spiel in einer WAITING-Lobby.
     * Prueft: Lobby-State wechselt auf IN_GAME (geprüft via ArgumentCaptor),
     * gameService.setupGame() wurde aufgerufen, GAME_START-Nachricht wurde gesendet.
     * Faengt Bug: Falscher State nach Start oder fehlendes save()/flush().
     */
    @Test
    void startGame_waitingLobby_transitionsToInGame() {
        Lobby lobby = buildLobby(LOBBY_ID, LobbyState.WAITING);
        Mockito.when(lobbyRepository.findById(LOBBY_ID)).thenReturn(Optional.of(lobby));

        lobbyService.startGame(LOBBY_ID);

        ArgumentCaptor<Lobby> lobbyCaptor = ArgumentCaptor.forClass(Lobby.class);
        Mockito.verify(lobbyRepository).save(lobbyCaptor.capture());
        assertEquals(LobbyState.IN_GAME, lobbyCaptor.getValue().getLobbyState());
        Mockito.verify(gameService).setupGame(lobby);

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(messagingTemplate, Mockito.times(2)).convertAndSend(
                Mockito.eq("/topic/lobby/" + LOBBY_ID), msgCaptor.capture());

        List<Message> sent = msgCaptor.getAllValues();
        assertEquals(MessageType.LOAD_GAME, sent.get(0).getType());
        assertEquals(MessageType.GAME_START, sent.get(1).getType());
    }

    /**
     * Szenario: startGame() wird aufgerufen, aber die Lobby ist bereits IN_GAME.
     * Prueft: Kein zweiter setupGame()-Aufruf, kein zweites save() — Idempotenz.
     * Faengt Bug: Ohne den Early-Return wuerden Runden doppelt angelegt.
     */
    @Test
    void startGame_alreadyInGame_doesNothing() {
        Lobby lobby = buildLobby(LOBBY_ID, LobbyState.IN_GAME);
        Mockito.when(lobbyRepository.findById(LOBBY_ID)).thenReturn(Optional.of(lobby));

        lobbyService.startGame(LOBBY_ID);

        Mockito.verify(gameService, Mockito.never()).setupGame(Mockito.any());
        Mockito.verify(lobbyRepository, Mockito.never()).save(Mockito.any());
    }

    // ═══════════════════════════════════════════════════════════════════
    // leaveLobby
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: Der Admin verlaesst eine Lobby mit einem weiteren Spieler.
     * Prueft: Admin wurde aus players entfernt, neuer Admin ist der verbliebene
     * Spieler (players.get(0) nach Entfernen), save() wurde aufgerufen.
     * Faengt Bug: Ohne Admin-Transfer haette die Lobby keinen Admin mehr —
     * nachfolgende Operationen, die lobby.getAdmin() aufrufen, wuerden NPE-en.
     */
    @Test
    void leaveLobby_adminLeavesWithOneOtherPlayer_transfersAdmin() {
        Lobby lobby = buildLobby(LOBBY_ID, LobbyState.WAITING);
        lobby.setAdmin(adminUser);
        lobby.getPlayers().add(adminUser);
        lobby.getPlayers().add(secondUser);
        Mockito.when(lobbyRepository.findById(LOBBY_ID)).thenReturn(Optional.of(lobby));

        lobbyService.leaveLobby(LOBBY_ID, ADMIN_ID);

        assertFalse(lobby.getPlayers().stream()
                .anyMatch(p -> p.getUserId().equals(ADMIN_ID)), "Admin must be removed");
        assertEquals(SECOND_USER_ID, lobby.getAdmin().getUserId(),
                "Admin role must transfer to the remaining player");
        Mockito.verify(lobbyRepository).save(lobby);
    }

    /**
     * Szenario: Der letzte User verlaesst die Lobby.
     * Prueft: lobbyRepository.delete() wird aufgerufen — keine leere Lobby
     * bleibt in der DB haengen.
     * Faengt Bug: Fehlendes delete() wuerde leere Lobbies akkumulieren und in
     * getAllLobbies() auftauchen.
     */
    @Test
    void leaveLobby_lastUserLeaves_deletesLobby() {
        Lobby lobby = buildLobby(LOBBY_ID, LobbyState.WAITING);
        lobby.setAdmin(adminUser);
        lobby.getPlayers().add(adminUser);
        Mockito.when(lobbyRepository.findById(LOBBY_ID)).thenReturn(Optional.of(lobby));

        lobbyService.leaveLobby(LOBBY_ID, ADMIN_ID);

        Mockito.verify(lobbyRepository).delete(lobby);
        Mockito.verify(lobbyRepository, Mockito.never()).save(Mockito.any());
    }

    /**
     * Szenario: Ein Non-Admin verlaesst die Lobby.
     * Prueft: Nur der ausgetretene User ist weg, Admin bleibt unveraendert.
     */
    @Test
    void leaveLobby_nonAdminLeaves_adminStaysTheSame() {
        Lobby lobby = buildLobby(LOBBY_ID, LobbyState.WAITING);
        lobby.setAdmin(adminUser);
        lobby.getPlayers().add(adminUser);
        lobby.getPlayers().add(secondUser);
        Mockito.when(lobbyRepository.findById(LOBBY_ID)).thenReturn(Optional.of(lobby));

        lobbyService.leaveLobby(LOBBY_ID, SECOND_USER_ID);

        assertFalse(lobby.getPlayers().stream()
                .anyMatch(p -> p.getUserId().equals(SECOND_USER_ID)));
        assertEquals(ADMIN_ID, lobby.getAdmin().getUserId(),
                "Admin must stay the same when a non-admin leaves");
    }

    // ═══════════════════════════════════════════════════════════════════
    // getLobby
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: Ein User, der NICHT in der players-Liste ist, fragt die Lobby an.
     * Prueft: 403 FORBIDDEN.
     * Faengt Bug: Ohne diesen Check koennte jeder User fremde Lobby-States einsehen.
     */
    @Test
    void getLobby_userNotInLobby_throwsForbidden() {
        Lobby lobby = buildLobby(LOBBY_ID, LobbyState.WAITING);
        lobby.setAdmin(adminUser);
        Mockito.when(lobbyRepository.findById(LOBBY_ID)).thenReturn(Optional.of(lobby));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.getLobby(LOBBY_ID, 999L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    /**
     * Szenario: Ein User, der in der players-Liste ist, fragt die Lobby an.
     * Prueft: Die Lobby wird ohne Fehler zurueckgegeben.
     * Faengt Bug: Invertierte existsUser-Pruefung wuerde legitime User abweisen.
     */
    @Test
    void getLobby_userInLobby_returnsLobby() {
        Lobby lobby = buildLobby(LOBBY_ID, LobbyState.WAITING);
        lobby.setAdmin(adminUser);
        lobby.getPlayers().add(secondUser);
        Mockito.when(lobbyRepository.findById(LOBBY_ID)).thenReturn(Optional.of(lobby));

        Lobby result = lobbyService.getLobby(LOBBY_ID, SECOND_USER_ID);

        assertEquals(LOBBY_ID, result.getLobbyId());
    }

    // ═══════════════════════════════════════════════════════════════════
    // getLobbyById
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Szenario: Abfrage einer Lobby-ID, die nicht in der DB existiert.
     * Prueft: 404 NOT_FOUND.
     * Faengt Bug: Ein stillschweigendes 'return null' wuerde NPEs beim Caller
     * ausloesen statt einer sauberen 404.
     */
    @Test
    void getLobbyById_unknownId_throwsNotFound() {
        Mockito.when(lobbyRepository.findById(9999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.getLobbyById(9999L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ═══════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════

    private User buildUser(Long id, String username, String token) {
        User user = new User();
        user.setUserId(id);
        UserProfile profile = new UserProfile();
        profile.setUsername(username);
        profile.setPassword("password");
        profile.setEmail(username + "@test.com");
        user.setUserProfile(profile);
        user.setIsOnline(false);
        user.setIsGuest(false);
        user.setToken(token);
        return user;
    }

    private Lobby buildLobby(Long id, LobbyState state) {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(id);
        lobby.setLobbyName("TestLobby");
        lobby.setLobbyCode(LOBBY_CODE);
        lobby.setMaxPlayers(4);
        lobby.setMaxRounds(5);
        lobby.setVisibility(LobbyVisibility.PUBLIC);
        lobby.setLobbyState(state);
        lobby.setCurrentRound(0);
        lobby.setPlayers(new ArrayList<>());
        return lobby;
    }
}
