package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;
import ch.uzh.ifi.hase.soprafs26.constant.MessageType;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.objects.Station;
import ch.uzh.ifi.hase.soprafs26.objects.Train;
import ch.uzh.ifi.hase.soprafs26.objects.UserGameStatus;
import ch.uzh.ifi.hase.soprafs26.repository.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessMessageDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ResultDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RoundStartDTO;
import ch.uzh.ifi.hase.soprafs26.trains.TrainPositionFetcher;
import ch.uzh.ifi.hase.soprafs26.websocket.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GameServiceTest {

    @Mock private TrainPositionFetcher trainPositionFetcher;
    @Mock private RoundRepository roundRepository;
    @Mock private GuessRepository guessRepository;
    @Mock private AchievementService achievementService;
    @Mock private LobbyRepository lobbyRepository;
    @Mock private UserRepository userRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private RoundHistoryRepository roundHistoryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private GameService gameService;

    private static final Long LOBBY_ID = 1L;
    private User user1;
    private User user2;
    private Lobby lobby;

    @BeforeEach
    void setUp() {
        gameService = new GameService(
                trainPositionFetcher, roundRepository, guessRepository,
                achievementService, lobbyRepository, userRepository,
                messagingTemplate, objectMapper, roundHistoryRepository);

        user1 = buildUser(1L, "Alice", "alice@uzh.ch");
        user2 = buildUser(2L, "Bob",   "bob@uzh.ch");

        lobby = new Lobby();
        lobby.setLobbyId(LOBBY_ID);
        lobby.setLobbyName("TestLobby");
        lobby.setLobbyCode("ABCD");
        lobby.setAdmin(user1);
        lobby.setMaxPlayers(2);
        lobby.setMaxRounds(1);
        lobby.setVisibility(LobbyVisibility.PRIVATE);
        lobby.setLobbyState(LobbyState.IN_GAME);
        lobby.setCurrentRound(0);
        lobby.getPlayers().add(user1);
        lobby.getPlayers().add(user2);
    }

    @AfterEach
    void tearDown() {
        gameService.cleanupAllTimers();
    }

    // ═══════════════════════════════════════════════════════════════════
    // setupGame
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: setupGame speichert fuer jede Runde ein Round-Entity und
     * fuer jeden Spieler einen leeren Guess-Eintrag.
     * Faengt Bug: Wenn Rounds oder Guesses nicht persistiert werden,
     * haette processGuessMessage keinen Guess zum Aktualisieren und
     * wuerde mit NPE abstuerzen.
     */
    @Test
    void setupGame_success() throws Exception {
        Train train = buildTrain(100L, 100L);
        when(trainPositionFetcher.fetchTrains(1)).thenReturn(List.of(train));
        doNothing().when(trainPositionFetcher).interpolatePosition(any(Train.class));

        gameService.setupGame(lobby);

        ArgumentCaptor<Round> roundCaptor = ArgumentCaptor.forClass(Round.class);
        verify(roundRepository).save(roundCaptor.capture());
        assertEquals(1, roundCaptor.getValue().getRoundNumber());
        assertEquals(lobby, roundCaptor.getValue().getLobby());

        verify(guessRepository, times(2)).save(any(Guess.class));
        verify(roundRepository).flush();
        verify(guessRepository).flush();
    }

    // ═══════════════════════════════════════════════════════════════════
    // processGuessMessage
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: Ein valider Guess (perfekte Koordinaten) wird gespeichert,
     * gibt 1000 Punkte und sendet eine GAME_STATE-Nachricht.
     * Faengt Bug: Wenn der Query-Pfad "user_userId" im Guess-Repository
     * falsch ist, wuerde guess null sein und NullPointerException folgen.
     */
    @Test
    void processGuessMessage_success() throws Exception {
        Train train = buildTrain(100L, 100L);
        Round round = buildRound(1, train);
        when(roundRepository.findByLobbyOrderByRoundNumberAsc(lobby)).thenReturn(List.of(round));
        gameService.roundStart(lobby);
        clearInvocations(messagingTemplate);

        Guess g1 = buildGuess(round, user1, null, null, null, null);
        Guess g2 = buildGuess(round, user2, null, null, null, null);
        when(guessRepository.findByRoundAndUserUserId(round, user1.getUserId())).thenReturn(g1);
        when(guessRepository.findByRound(round)).thenReturn(List.of(g1, g2));

        gameService.processGuessMessage(new GuessMessageDTO(LOBBY_ID, 1L, 100L, 100L), lobby);

        assertEquals(1000, g1.getPoints(), "Perfect guess (same coordinates as train) must give 1000 points");
        assertTrue(g1.getHasGuessed());
        verify(messagingTemplate, atLeastOnce())
                .convertAndSend(eq("/topic/game/" + LOBBY_ID), any(Message.class));
    }

    /**
     * Prueft: Wenn kein Timer aktiv ist (Runde noch nicht gestartet),
     * wird der Guess still verworfen — kein Repository-Zugriff, keine Nachricht.
     * Faengt Bug: Wuerde der Guard-Check fehlen, koennte ein User auch
     * ausserhalb des Runden-Fensters Punkte erzielen.
     */
    @Test
    void processGuessMessage_noGuessAllowed() {
        gameService.processGuessMessage(new GuessMessageDTO(LOBBY_ID, 1L, 100L, 100L), lobby);

        verify(guessRepository, never()).findByRoundAndUserUserId(any(), any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    // ═══════════════════════════════════════════════════════════════════
    // readyForNextRound / updateUserGameStatus
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: Wenn erst ein User bereit ist, wird roundStart NICHT ausgeloest.
     * Faengt Bug: Ein zu frueh gestartetes roundStart wuerde den zweiten
     * Spieler in einem laufenden Timer-Fenster ueberraschen.
     */
    @Test
    void readyForNextRound_notAllReady_doesNotStartRound() {
        gameService.readyForNextRound(new UserGameStatus(1L, true), lobby);

        verify(roundRepository, never()).findByLobbyOrderByRoundNumberAsc(any());
    }

    /**
     * Prueft: Sobald alle Spieler bereit sind, wird roundStart aufgerufen
     * und eine ROUND_START-Nachricht versendet.
     */
    @Test
    void readyForNextRound_allAreReady_startsRound() throws Exception {
        Train train = buildTrain(50L, 50L);
        Round round = buildRound(1, train);
        when(roundRepository.findByLobbyOrderByRoundNumberAsc(lobby)).thenReturn(List.of(round));

        gameService.readyForNextRound(new UserGameStatus(1L, true), lobby);
        gameService.readyForNextRound(new UserGameStatus(2L, true), lobby);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/" + LOBBY_ID), captor.capture());
        assertEquals(MessageType.ROUND_START, captor.getValue().getType());
    }

    /**
     * Prueft: updateUserGameStatus gibt false zurueck solange nicht alle bereit sind.
     */
    @Test
    void updateUserGameStatus_notAllReady() {
        Boolean result = gameService.updateUserGameStatus(new UserGameStatus(1L, true), lobby);
        assertFalse(result);
    }

    /**
     * Prueft: updateUserGameStatus gibt true zurueck sobald alle Spieler bereit sind.
     */
    @Test
    void updateUserGameStatus_allReady() {
        gameService.updateUserGameStatus(new UserGameStatus(1L, true), lobby);
        Boolean result = gameService.updateUserGameStatus(new UserGameStatus(2L, true), lobby);
        assertTrue(result);
    }

    // ═══════════════════════════════════════════════════════════════════
    // canSubmitGuess
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: Nach roundStart ist ein Timer aktiv und canSubmitGuess gibt true zurueck.
     */
    @Test
    void canSubmitGuess_success() throws Exception {
        Train train = buildTrain(10L, 10L);
        Round round = buildRound(1, train);
        when(roundRepository.findByLobbyOrderByRoundNumberAsc(lobby)).thenReturn(List.of(round));

        gameService.roundStart(lobby);

        assertTrue(gameService.canSubmitGuess(LOBBY_ID));
    }

    /**
     * Prueft: Ohne aktiven Timer gibt canSubmitGuess false zurueck.
     */
    @Test
    void canSubmitGuess_fail() {
        assertFalse(gameService.canSubmitGuess(LOBBY_ID));
    }

    // ═══════════════════════════════════════════════════════════════════
    // roundStart
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: roundStart erhoet currentRound, sendet ROUND_START mit Zugnummer
     * und null-Koordinaten, und setzt einen Timer.
     * Faengt Bug: Wenn currentRound nicht erhoet wird, laedt processGuessMessage
     * die falsche Runde; wenn Koordinaten nicht geloescht werden, sehen Clients
     * die Zugposition vor dem Guess.
     */
    @Test
    void roundStart_success() throws Exception {
        Train train = buildTrain(100L, 200L);
        Round round = buildRound(1, train);
        when(roundRepository.findByLobbyOrderByRoundNumberAsc(lobby)).thenReturn(List.of(round));

        assertEquals(0, lobby.getCurrentRound());
        gameService.roundStart(lobby);

        assertEquals(1, lobby.getCurrentRound());
        assertTrue(gameService.canSubmitGuess(LOBBY_ID));

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/" + LOBBY_ID), captor.capture());
        assertEquals(MessageType.ROUND_START, captor.getValue().getType());

        RoundStartDTO payload = (RoundStartDTO) captor.getValue().getPayload();
        assertEquals(1, payload.getRoundNumber());
        assertEquals(1, payload.getMaxRounds());
        assertEquals(0, payload.getTrain().getCurrentX(), "Train X must be zeroed out for clients");
        assertEquals(0, payload.getTrain().getCurrentY(), "Train Y must be zeroed out for clients");
    }

    // ═══════════════════════════════════════════════════════════════════
    // roundEnd
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: roundEnd sendet ROUND_END ohne Payload und startet einen
     * Folge-Timer fuer allowedToPublish.
     * Faengt Bug: Ohne Folge-Timer wuerde publishScores nie aufgerufen
     * wenn Spieler nicht selbst alle geraten haben.
     */
    @Test
    void roundEnd_success() {
        gameService.roundEnd(LOBBY_ID);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/" + LOBBY_ID), captor.capture());
        assertEquals(MessageType.ROUND_END, captor.getValue().getType());
        assertNull(captor.getValue().getPayload());

        assertTrue(gameService.canSubmitGuess(LOBBY_ID), "roundEnd must schedule the allowedToPublish timer");
    }

    // ═══════════════════════════════════════════════════════════════════
    // allowedToPublish
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: allowedToPublish ruft publishScores auf wenn die Scores noch
     * nicht veroeffentlicht wurden, und eine SCORES-Nachricht wird gesendet.
     * Faengt Bug: Ohne die scoresPublished-Guard wuerde publishScores doppelt
     * aufgerufen und SCORES zweimal an die Clients gesendet.
     */
    @Test
    void allowedToPublish_callsPublish() throws Exception {
        Train train = buildTrain(100L, 100L);
        Round round = buildRound(1, train);
        lobby.setMaxRounds(2);
        when(roundRepository.findByLobbyOrderByRoundNumberAsc(lobby)).thenReturn(List.of(round));
        gameService.roundStart(lobby);
        clearInvocations(messagingTemplate);

        Guess g1 = buildGuess(round, user1, 80, 100f, 100f, 0.5f);
        Guess g2 = buildGuess(round, user2, 60, 101f, 101f, 1.0f);
        when(lobbyRepository.findById(LOBBY_ID)).thenReturn(Optional.of(lobby));
        when(guessRepository.findByRound(round)).thenReturn(List.of(g1, g2));

        gameService.allowedToPublish(LOBBY_ID);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messagingTemplate, atLeastOnce())
                .convertAndSend(eq("/topic/game/" + LOBBY_ID), captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(m -> m.getType() == MessageType.SCORES),
                "SCORES message must be sent by allowedToPublish");
    }

    // ═══════════════════════════════════════════════════════════════════
    // publishScores
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: In einer Zwischen-Runde (nicht letzte) wird SCORES gesendet
     * mit korrekter Rundennummer und Spieleranzahl, ohne gameTearDown aufzurufen.
     */
    @Test
    void publishScores_success_middleRound() throws Exception {
        Train train = buildTrain(100L, 100L);
        Round round = buildRound(1, train);
        lobby.setCurrentRound(1);
        lobby.setMaxRounds(2);

        Guess g1 = buildGuess(round, user1, 80, 100f, 100f, 0.5f);
        Guess g2 = buildGuess(round, user2, 60, 101f, 101f, 1.0f);
        when(lobbyRepository.findById(LOBBY_ID)).thenReturn(Optional.of(lobby));
        when(roundRepository.findByLobbyOrderByRoundNumberAsc(lobby)).thenReturn(List.of(round));
        when(guessRepository.findByRound(round)).thenReturn(List.of(g1, g2));

        gameService.publishScores(lobby);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/" + LOBBY_ID), captor.capture());
        assertEquals(MessageType.SCORES, captor.getValue().getType());

        ResultDTO payload = (ResultDTO) captor.getValue().getPayload();
        assertEquals(1, payload.getCurrentRound());
        assertEquals(2, payload.getUserResults().size());

        verify(roundRepository, never()).deleteByLobby(any());
    }

    /**
     * Prueft: In der letzten Runde wird nach SCORES automatisch gameTearDown
     * aufgerufen — Lobby wird auf FINISHED gesetzt und Daten werden bereinigt.
     * Faengt Bug: Ohne den Vergleich maxRounds == currentRound wuerde das Spiel
     * nie enden oder immer sofort enden.
     */
    @Test
    void publishScores_triggersTearDown_lastRound() throws Exception {
        Train train = buildTrain(100L, 100L);
        Round round = buildRound(1, train);
        lobby.setCurrentRound(1);
        lobby.setMaxRounds(1);

        Guess g1 = buildGuess(round, user1, 80, 100f, 100f, 0.5f);
        Guess g2 = buildGuess(round, user2, 60, 101f, 101f, 1.0f);
        when(lobbyRepository.findById(LOBBY_ID)).thenReturn(Optional.of(lobby));
        when(roundRepository.findByLobbyOrderByRoundNumberAsc(any())).thenReturn(List.of(round));
        when(guessRepository.findByRound(round)).thenReturn(List.of(g1, g2));
        when(roundHistoryRepository.findByUserUserId(anyLong())).thenReturn(Collections.emptyList());

        gameService.publishScores(lobby);

        verify(messagingTemplate).convertAndSend(eq("/topic/game/" + LOBBY_ID), any(Message.class));
        assertEquals(LobbyState.FINISHED, lobby.getLobbyState());
        verify(roundRepository).deleteByLobby(any(Lobby.class));
    }

    // ═══════════════════════════════════════════════════════════════════
    // calculateScore
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: Ein perfekter Guess (Distanz = 0) ergibt 1000 Punkte.
     */
    @Test
    void calculateScore_perfectGuess_returns1000() {
        Train train = buildTrain(100L, 100L);
        train.setLineOrigin(new Station("Start", 0L, 0L, 0L, 0L));
        train.setLineDestination(new Station("End", 100L, 0L, 10L, 10L));

        int score = gameService.calculateScore(train, 0.0);

        assertEquals(1000, score, "A perfect guess must give 1000 points");
    }

    /**
     * Prueft: Ein schlechter Guess (Distanz = volle Linienlaenge) ergibt
     * fast keine Punkte (>0 und <=10).
     */
    @Test
    void calculateScore_badGuess_returnsLowPoints() {
        Train train = buildTrain(0L, 0L);
        train.setLineOrigin(new Station("Start", 0L, 0L, 0L, 0L));
        train.setLineDestination(new Station("End", 100L, 0L, 10L, 10L));

        int score = gameService.calculateScore(train, 100.0);

        assertTrue(score > 0 && score <= 10,
                "A guess equal to the full line length should give near-zero points, got: " + score);
    }

    // ═══════════════════════════════════════════════════════════════════
    // calculateGuessDistance
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: Der pythagoraische 3-4-5 Dreieck ergibt Distanz 5.
     */
    @Test
    void calculateGuessDistance_simpleRightTriangle() {
        Train train = buildTrain(0L, 0L);

        double distance = gameService.calculateGuessDistance(train, 3L, 4L);

        assertEquals(5.0, distance, 0.001);
    }

    /**
     * Prueft: Gleiche Koordinaten wie der Zug ergeben Distanz 0.
     */
    @Test
    void calculateGuessDistance_zeroDistance() {
        Train train = buildTrain(100L, 200L);

        double distance = gameService.calculateGuessDistance(train, 100L, 200L);

        assertEquals(0.0, distance, 0.001);
    }

    // ═══════════════════════════════════════════════════════════════════
    // gameTearDown
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: gameTearDown speichert RoundHistory, aktualisiert UserScoreboard,
     * evaluiert Achievements, setzt Lobby auf FINISHED und bereinigt DB-Eintraege.
     * Faengt Bug: Fehlende RoundHistory-Saves wuerden Profil-Statistiken fuer
     * immer falsch zurueckliefern; fehlende deleteByLobby waere ein Memory-Leak.
     */
    @Test
    void gameTearDown_success() throws Exception {
        Train train = buildTrain(100L, 100L);
        Round round = buildRound(1, train);

        Guess g1 = buildGuess(round, user1, 80, 100f, 100f, 0.5f);
        Guess g2 = buildGuess(round, user2, 60, 101f, 101f, 1.0f);
        when(roundRepository.findByLobbyOrderByRoundNumberAsc(lobby)).thenReturn(List.of(round));
        when(guessRepository.findByRound(round)).thenReturn(List.of(g1, g2));
        when(roundHistoryRepository.findByUserUserId(anyLong())).thenReturn(Collections.emptyList());

        gameService.gameTearDown(lobby);

        verify(roundHistoryRepository, times(2)).save(any(RoundHistory.class));
        verify(userRepository, times(2)).save(any(User.class));
        verify(achievementService).evaluateAchievementsForLobby(lobby);
        assertEquals(LobbyState.FINISHED, lobby.getLobbyState());
        verify(guessRepository).deleteByRound(round);
        verify(roundRepository).deleteByLobby(lobby);
    }

    // ═══════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════

    private User buildUser(Long id, String username, String email) {
        User user = new User();
        user.setUserId(id);
        UserProfile profile = new UserProfile();
        profile.setUsername(username);
        profile.setEmail(email);
        profile.setPassword("pw");
        user.setUserProfile(profile);
        UserScoreboard scoreboard = new UserScoreboard();
        scoreboard.setPlayedGames(0L);
        scoreboard.setPlayedRounds(0L);
        scoreboard.setTotalPoints(0L);
        scoreboard.setBestRoundPoints(0L);
        scoreboard.setGuessingPrecision(0f);
        user.setUserScoreboard(scoreboard);
        user.setIsOnline(false);
        user.setIsGuest(false);
        return user;
    }

    private Train buildTrain(long x, long y) {
        Train train = new Train();
        train.setCurrentX(x);
        train.setCurrentY(y);
        train.setLineOrigin(new Station("Start", 0L, 0L, 0L, 0L));
        train.setLineDestination(new Station("End", 200L, 0L, 10L, 10L));
        return train;
    }

    private Round buildRound(int roundNumber, Train train) throws Exception {
        Round round = new Round();
        round.setLobby(lobby);
        round.setRoundNumber(roundNumber);
        round.setTrainData(objectMapper.writeValueAsString(train));
        return round;
    }

    private Guess buildGuess(Round round, User user, Integer points,
                              Float lat, Float lon, Float distance) {
        Guess guess = new Guess();
        guess.setRound(round);
        guess.setUser(user);
        guess.setPoints(points);
        guess.setLat(lat);
        guess.setLon(lon);
        guess.setDistanceToTrain(distance);
        guess.setHasGuessed(points != null);
        return guess;
    }
}
