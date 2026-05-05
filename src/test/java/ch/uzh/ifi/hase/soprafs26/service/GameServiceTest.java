package ch.uzh.ifi.hase.soprafs26.service;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;
import ch.uzh.ifi.hase.soprafs26.constant.MessageType;
import ch.uzh.ifi.hase.soprafs26.entity.GameResult;
import ch.uzh.ifi.hase.soprafs26.events.GameEndedEvent;
import ch.uzh.ifi.hase.soprafs26.objects.Lobby;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import ch.uzh.ifi.hase.soprafs26.websocket.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.objects.*;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.trains.TrainPositionFetcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;


import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TrainPositionFetcher trainPositionFetcher;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ObjectMapper objectMapper;

    @InjectMocks
    private GameService gameService;

    private static Lobby getLobby() {
        User user1 = new User();
        user1.setUserId(1L);
        User user2 = new User();
        user2.setUserId(2L);

        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);
        lobby.setLobbyName("TestLobby");
        lobby.setLobbyCode("valid-lobbyCode");

        Admin newAdmin = new Admin(user1.getUserId(), "valid-token");
        lobby.setAdmin(newAdmin);

        lobby.setSize(2);
        lobby.setVisibility(LobbyVisibility.PRIVATE);

        Map<Long, User> users = new HashMap<>();
        users.put(user1.getUserId(), user1);
        users.put(user2.getUserId(), user2);
        lobby.setUsers(users);

        lobby.setCurrentRound(0);
        lobby.setMaxRounds(1);

        lobby.setScores(new HashMap<>());
        Score score1 = new Score(user1.getUserId());
        score1.setPoints(0);
        lobby.setScore(user1.getUserId(), score1);
        Score score2 = new Score(user2.getUserId());
        score2.setPoints(0);
        lobby.setScore(user2.getUserId(), score2);

        lobby.setLobbyState(LobbyState.WAITING);
        return lobby;
    }

    private Game getGame(Lobby lobby) throws Exception {
        Train mockTrain = new Train();
        mockTrain.setCurrentX(100L);
        mockTrain.setCurrentY(100L);
        mockTrain.setLineOrigin(new Station("Start", 0L, 0L, 0, 0));
        mockTrain.setLineDestination(new Station("End", 200L, 200L, 10, 10));
        List<Train> trains = new ArrayList<>();

        for(int i=0; i<lobby.getMaxRounds(); i++) {
            trains.add(mockTrain);
        }

        lenient().when(trainPositionFetcher.fetchTrains(anyInt())).thenReturn(trains);
        lenient().doNothing().when(trainPositionFetcher).interpolatePosition(any(Train.class));


        return gameService.setupGame(lobby);
    }

    @AfterEach
    void tearDown() {
        gameService.cleanupAllTimers();
        gameService.cleanupGames();
    }


    @Test
    void getGameById_success() throws Exception {

        Lobby lobby = getLobby();

        Game game = getGame(lobby);
        lobby.setGame(game);

        Long gameId = game.getGameId();

        Game foundGame = gameService.getGameById(gameId);

        assertNotNull(foundGame);
        assertEquals(gameId, foundGame.getGameId());
        assertEquals(game, foundGame);
    }

    @Test
    void getGameById_notFound_throwsException() {

        Long nonExistentId = 999L;

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.getGameById(nonExistentId);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Game not found", exception.getReason());
    }

    @Test
    void setupGame_success() throws Exception {

        Lobby lobby = getLobby();

        Train mockTrain = new Train();
        mockTrain.setCurrentX(100L);
        mockTrain.setCurrentY(100L);
        mockTrain.setLineOrigin(new Station("Start", 0L, 0L, 0, 0));
        mockTrain.setLineDestination(new Station("End", 200L, 200L, 10, 10));

        lenient().when(trainPositionFetcher.fetchTrains(anyInt())).thenReturn(List.of(mockTrain));
        lenient().doNothing().when(trainPositionFetcher).interpolatePosition(any(Train.class));


        Game game = gameService.setupGame(lobby);
        lobby.setGame(game);


        assertNotNull(game);
        assertEquals(1L, game.getGameId());
        assertEquals(1, game.getRounds().size());

        Round firstRound = game.getRounds().get(0);
        assertNotNull(firstRound.getScores().get(1L));
        assertNotNull(firstRound.getAllUserGameStatuses().get(2L));


        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/lobby/1"), messageCaptor.capture());

        Message sentMessage = messageCaptor.getValue();

        assertEquals(MessageType.GAME_START, sentMessage.getType());

        MyLobbyDTO payload = (MyLobbyDTO) sentMessage.getPayload();
        assertEquals(lobby.getAdmin(), payload.getAdmin());
        assertEquals(lobby.getLobbyId(), payload.getLobbyId());

    }

    @Test
    void processGuessMessage_success() throws Exception {
        Lobby lobby = getLobby();

        Game game = getGame(lobby);
        lobby.setGame(game);

        gameService.roundStart(lobby);

        GuessMessageDTO guessMessage = new GuessMessageDTO(1L, 1L);
        guessMessage.setXcoordinate(100L); guessMessage.setYcoordinate(100L);

        gameService.processGuessMessage(guessMessage, lobby);

        assertEquals(1000, game.getRounds().get(0).getScores().get(1L).getPoints(), "ist es hier?");
        assertEquals(1000, lobby.getScore(1L).getPoints(), "oder ist es hier?");

        assertTrue(game.getRounds().get(0).getAllUserGameStatuses().get(1L).getIsReady());


        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/game/1"), messageCaptor.capture());

        List<Message> capturedMessages = messageCaptor.getAllValues();

        boolean hasSate = capturedMessages.stream().anyMatch(message -> message.getType() == MessageType.GAME_STATE);
        assertTrue(hasSate);

    }

    @Test
    void processGuessMessage_noGuessAllowed() throws Exception {
        Lobby lobby = getLobby();

        Game game  = getGame(lobby);
        lobby.setGame(game);

        GuessMessageDTO guessMessage = new GuessMessageDTO(1L, 1L);
        guessMessage.setXcoordinate(100L); guessMessage.setYcoordinate(100L);

        Mockito.clearInvocations(messagingTemplate);
        gameService.processGuessMessage(guessMessage, lobby);


        Score roundScore = game.getRounds().get(0).getScores().get(1L);
        assertNull(roundScore.getPoints());

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Message.class));
    }

    @Test
    void readyForNextRound_noGame_notAllReady() throws Exception {
        Lobby lobby =  getLobby();

        Game game = getGame(lobby);

        UserGameStatus userGameStatus = new UserGameStatus(1L, true);

        assertNull(lobby.getGame());
        gameService.readyForNextRound(userGameStatus, lobby);
        assertEquals(gameService.getGameById(lobby.getLobbyId()), lobby.getGame());

        Mockito.clearInvocations(messagingTemplate);
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Message.class));

    }

    @Test
    void readyForNextRound_allAreReady() throws Exception {
        Lobby lobby =  getLobby();

        Game game  = getGame(lobby);
        lobby.setGame(game);

        gameService.readyForNextRound(new UserGameStatus(1L, true),  lobby);

        gameService.readyForNextRound(new UserGameStatus(2L, true),  lobby);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/game/1"), messageCaptor.capture());

        List<Message> capturedMessages = messageCaptor.getAllValues();

        boolean hasRoundStart = capturedMessages.stream().anyMatch(message -> message.getType() == MessageType.ROUND_START);
        assertTrue(hasRoundStart);
    }

    @Test
    void updateUserGameStatus_roundZero_NotAllReady() throws Exception {
        Lobby lobby =  getLobby();
        Game game = getGame(lobby);
        lobby.setGame(game);

        lobby.setCurrentRound(0);
        Boolean allAreReady = gameService.updateUserGameStatus(new UserGameStatus(1L, true), lobby);
        assertFalse(allAreReady);
        assertTrue(game.getConnectedPlayers().get(0).getIsReady(), "Player is still not ready");
    }

    @Test
    void updateUserGameStatus_roundZero_AllReady() throws Exception {
        Lobby lobby =  getLobby();
        Game game = getGame(lobby);
        lobby.setGame(game);

        lobby.setCurrentRound(0);
        Boolean firstPlayerReady = gameService.updateUserGameStatus(new UserGameStatus(1L, true),  lobby);
        Boolean allAreReady = gameService.updateUserGameStatus(new UserGameStatus(2L, true), lobby);
        assertTrue(allAreReady);
        assertTrue(game.getConnectedPlayers().stream().allMatch(UserGameStatus::getIsReady), "not all Players are connected");
        assertEquals(2, game.getConnectedPlayers().size());
    }

    @Test
    void updateUserGameStatus_roundOne_NotAllReady() throws Exception {
        Lobby lobby =  getLobby();
        Game game = getGame(lobby);
        lobby.setGame(game);

        lobby.setCurrentRound(1);
        Boolean allAreReady = gameService.updateUserGameStatus(new UserGameStatus(1L, true), lobby);
        assertFalse(allAreReady);
        assertTrue(game.getRounds().get(0).getAllUserGameStatuses().get(1L).getIsReady(), "Player is still not ready");
        assertFalse(game.getRounds().get(0).getAllUserGameStatusesList().stream().allMatch(UserGameStatus::getIsReady), "All Players are ready, which is false");
    }

    @Test
    void updateUserGameStatus_roundOne_AllReady() throws Exception {
        Lobby lobby =  getLobby();
        Game game = getGame(lobby);
        lobby.setGame(game);

        lobby.setCurrentRound(1);
        Boolean firstPlayerReady = gameService.updateUserGameStatus(new UserGameStatus(1L, true),  lobby);
        Boolean allAreReady = gameService.updateUserGameStatus(new UserGameStatus(2L, true), lobby);
        assertTrue(allAreReady);
        assertTrue(game.getRounds().get(0).getAllUserGameStatusesList().stream().allMatch(UserGameStatus::getIsReady), "not all Players are ready");
    }

    @Test
    void canSubmitGuess_success() throws Exception {
        Lobby lobby =  getLobby();
        Game game = getGame(lobby);
        lobby.setGame(game);

        gameService.roundStart(lobby);
        boolean canSubmit = gameService.canSubmitGuess(game.getGameId());
        assertTrue(canSubmit);
    }

    @Test
    void canSubmitGuess_fail() throws Exception {
        Lobby lobby =  getLobby();
        Game game = getGame(lobby);
        lobby.setGame(game);

        boolean canSubmit = gameService.canSubmitGuess(game.getGameId());
        assertFalse(canSubmit);
    }

    @Test
    void roundStart_success() throws Exception {
        Lobby lobby =  getLobby();
        Game game = getGame(lobby);
        lobby.setGame(game);

        assertEquals(0, lobby.getCurrentRound());
        Long gameId = game.getGameId();

        Mockito.clearInvocations(messagingTemplate);
        gameService.roundStart(lobby);

        assertEquals(1, lobby.getCurrentRound(), "Round was not incremented correctly");
        assertTrue(gameService.canSubmitGuess(gameId), "No timer initialized");

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/1"),  messageCaptor.capture());

        Message sentMessage = messageCaptor.getValue();
        assertEquals(MessageType.ROUND_START, sentMessage.getType());

        RoundStartDTO payload = (RoundStartDTO) sentMessage.getPayload();
        assertEquals(1, payload.getRoundNumber());
        assertEquals(lobby.getMaxRounds(), payload.getMaxRounds());

        assertEquals(0, payload.getTrain().getCurrentX());
        assertEquals(0, payload.getTrain().getCurrentY());
    }

    @Test
    void roundEnd_success() throws Exception {
        Lobby lobby =  getLobby();
        Game game = getGame(lobby);
        lobby.setGame(game);

        lobby.setCurrentRound(1);
        gameService.roundEnd(lobby);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/1"),  messageCaptor.capture());

        Message sentMessage = messageCaptor.getValue();
        assertEquals(MessageType.ROUND_END, sentMessage.getType());
        assertNull(sentMessage.getPayload());

        assertTrue(gameService.canSubmitGuess(game.getGameId()),  "No timer initialized");
    }

    @Test
    void allowedToPublish_callsPublish() throws Exception {
        Lobby lobby =  getLobby();
        Game game = getGame(lobby);
        lobby.setGame(game);
        lobby.setCurrentRound(1);

        GameResult gameResult = new GameResult();
        gameResult.setGameId(game.getGameId());
        gameResult.setRounds(game.getRounds());
        gameResult.setScores(lobby.getScores());

        when(gameRepository.findByGameId(game.getGameId())).thenReturn(gameResult);

        User user1 = new User();
        user1.setUserId(1L);
        User user2 = new User();
        user2.setUserId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        gameService.allowedToPublish(lobby);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/game/1"),  messageCaptor.capture());

        assertTrue(messageCaptor.getAllValues().stream().anyMatch(message -> message.getType() == MessageType.SCORES), "Scores message was not sent");
    }

//    @Test
//    void allowedToPublish_doesNothing() throws Exception {
//        Lobby lobby =  getLobby();
//        Game game = getGame(lobby);
//        lobby.setGame(game);
//        lobby.setCurrentRound(1);
//
//        GameResult mockResult = new GameResult();
//        when(gameRepository.findByGameId(game.getGameId())).thenReturn(mockResult);
//        User user = new User(); user.setUserId(1L);
//        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
//
//        gameService.allowedToPublish(lobby);
//
//        Mockito.clearInvocations(messagingTemplate);
//
//        gameService.allowedToPublish(lobby);
//
//        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Message.class));
//    }

    @Test
    void publishScores_success_middleRound() throws Exception {
        Lobby lobby = getLobby();

        lobby.setMaxRounds(2);

        Game game = getGame(lobby);
        lobby.setGame(game);
        lobby.setCurrentRound(1);


        Round currentRound = game.getRounds().get(0);
        GuessMessageDTO guess = new GuessMessageDTO(1L, 1L);
        guess.setXcoordinate(100L);
        guess.setYcoordinate(100L);
        currentRound.setGuessMessage(1L, guess);
        currentRound.setDistances(1L, 0.0);
        currentRound.setScore(1L, 1000);


        gameService.publishScores(lobby);


        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/1"), messageCaptor.capture());

        Message sentMessage = messageCaptor.getValue();
        assertEquals(MessageType.SCORES, sentMessage.getType());

        ResultDTO payload = (ResultDTO) sentMessage.getPayload();
        assertEquals(1, payload.getCurrentRound());

        assertEquals(2, payload.getUserResults().size());


        verify(gameRepository, never()).save(any(GameResult.class));
    }

    @Test
    void publishScores_triggersTearDown_lastRound() throws Exception {

        Lobby lobby = getLobby();
        lobby.setMaxRounds(1);

        Game game = getGame(lobby);
        lobby.setGame(game);
        lobby.setCurrentRound(1);


        GameResult mockResult = new GameResult();
        when(gameRepository.findByGameId(anyLong())).thenReturn(mockResult);

        User u1 = new User(); u1.setUsername("User1");
        User u2 = new User(); u2.setUsername("User2");
        when(userRepository.findById(1L)).thenReturn(Optional.of(u1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(u2));


        gameService.publishScores(lobby);


        verify(messagingTemplate).convertAndSend(eq("/topic/game/1"), any(Message.class));


        verify(gameRepository).save(any(GameResult.class));
        verify(eventPublisher).publishEvent(any(GameEndedEvent.class));


        assertNull(lobby.getGame());
    }

    @Test
    void calculateScore_perfectGuess_returns1000() {

        Train train = new Train();

        train.setLineOrigin(new Station("Start", 0L, 0L, 0, 0));
        train.setLineDestination(new Station("End", 100L, 100L, 10, 10));

        double guessDistance = 0.0; // Perfekt getroffen


        int score = gameService.calculateScore(train, guessDistance);


        assertEquals(1000, score, "Ein perfekter Guess muss 1000 Punkte geben.");
    }

    @Test
    void calculateScore_badGuess_returnsLowPoints() {

        Train train = new Train();

        train.setLineOrigin(new Station("Start", 0L, 0L, 0, 0));
        train.setLineDestination(new Station("End", 100L, 0L, 10, 10));

        double guessDistance = 100.0;

        int score = gameService.calculateScore(train, guessDistance);


        assertTrue(score > 0 && score <= 10, "Ein Guess, der um die volle Linienlänge daneben liegt, sollte minimal Punkte geben (ca. 5).");
    }

//    @Test
//    void calculateScore_degenerateLine_usesFallback() {
//
//        Train train = new Train();
//
//        train.setLineOrigin(new Station("Same", 100L, 100L, 0, 0));
//        train.setLineDestination(new Station("Same", 100L, 100L, 0, 0));
//
//        double guessDistance = 500.0;
//
//        int score = gameService.calculateScore(train, guessDistance);
//
//        assertTrue(score > 0);
//        assertEquals(266, score);
//    }

    @Test
    void calculateGuessDistance_simpleRightTriangle() {

        Train train = new Train();
        train.setCurrentX(0L);
        train.setCurrentY(0L);

        Long playerX = 3L;
        Long playerY = 4L;

        double distance = gameService.calculateGuessDistance(train, playerX, playerY);

        assertEquals(5.0, distance, 0.001);
    }

    @Test
    void calculateGuessDistance_zeroDistance() {
        Train train = new Train();
        train.setCurrentX(100L);
        train.setCurrentY(200L);

        Long playerX = 100L;
        Long playerY = 200L;

        double distance = gameService.calculateGuessDistance(train, playerX, playerY);

        assertEquals(0.0, distance, 0.001, "Bei identischen Koordinaten muss die Distanz 0 sein.");
    }

    @Test
    void gameTearDown_success() {
        Lobby lobby = getLobby();

        Game game = mock(Game.class);
        lobby.setGame(game);

        Long gameId = 1L;
        when(game.getGameId()).thenReturn(gameId);

        List<Round> rounds = new ArrayList<>();
        when(game.getRounds()).thenReturn(rounds);

        GameResult mockResult = new GameResult();
        when(gameRepository.findByGameId(gameId)).thenReturn(mockResult);

        User user1 = new User(); user1.setUserId(1L); user1.setUsername("Alice");
        User user2 = new User(); user2.setUserId(2L); user2.setUsername("Bob");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        gameService.gameTearDown(lobby);


        verify(gameRepository).save(any(GameResult.class));
        verify(gameRepository).flush();

        verify(eventPublisher).publishEvent(any(GameEndedEvent.class));

        assertNull(lobby.getGame(), "Das Game-Objekt sollte von der Lobby entkoppelt sein");

        assertEquals("Alice", mockResult.getUsernames().get(1L));
        assertEquals("Bob", mockResult.getUsernames().get(2L));


        assertFalse(gameService.canSubmitGuess(gameId));
    }
}