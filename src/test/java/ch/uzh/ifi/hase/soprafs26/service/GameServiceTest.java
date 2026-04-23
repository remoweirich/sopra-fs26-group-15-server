package ch.uzh.ifi.hase.soprafs26.service;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;
import ch.uzh.ifi.hase.soprafs26.constant.MessageType;
import ch.uzh.ifi.hase.soprafs26.objects.Lobby;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessMessageDTO;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MyLobbyDTO;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        lobby.setMaxRounds(3);

        lobby.setScores(new HashMap<>());
        lobby.setScore(user1.getUserId(), new Score(1L));
        lobby.setScore(user2.getUserId(), new Score(2L));

        lobby.setLobbyState(LobbyState.WAITING);
        return lobby;
    }

    @AfterEach
    void tearDown() {
        gameService.cleanupAllTimers();
        gameService.cleanupGames();
    }


    @Test
    void getGameById() {
    }

    @Test
    void setupGame_success() throws Exception {

        Lobby lobby = getLobby();

        List<Train> mockTrains = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Train train = new Train();
            train.setLineOrigin(new Station("Start", 0L, 0L, 0, 0));
            train.setLineDestination(new Station("End", 100L, 100L, 10, 10));
            mockTrains.add(train);
        }

        when(trainPositionFetcher.fetchTrainsLive(anyInt())).thenReturn(mockTrains);
        when(trainPositionFetcher.fetchTrainsMock(anyInt())).thenReturn(mockTrains);
        doNothing().when(trainPositionFetcher).interpolatePosition(any(Train.class));


        Game resultGame = gameService.setupGame(lobby);


        assertNotNull(resultGame);
        assertEquals(1L, resultGame.getGameId());
        assertEquals(3, resultGame.getRounds().size());

        Round firstRound = resultGame.getRounds().get(0);
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

        Train mockTrain = new Train();
        mockTrain.setCurrentX(100L);
        mockTrain.setCurrentY(100L);
        mockTrain.setLineOrigin(new Station("Start", 0L, 0L, 0, 0));
        mockTrain.setLineDestination(new Station("End", 200L, 200L, 10, 10));

        when(trainPositionFetcher.fetchTrainsMock(anyInt())).thenReturn(List.of(mockTrain));
        when(trainPositionFetcher.fetchTrainsLive(anyInt())).thenReturn(List.of(mockTrain));
        Game game = gameService.setupGame(lobby);

        gameService.roundStart(lobby);


        GuessMessageDTO guessMessage = new GuessMessageDTO(1L, 1L);
        guessMessage.setXcoordinate(100L); guessMessage.setYcoordinate(100L);


        gameService.processGuessMessage(guessMessage, lobby);

        assertEquals(1000, game.getRounds().get(0).getScores().get(1L).getPoints());
        assertEquals(1000, lobby.getScore(1L).getPoints());

        assertEquals(true, game.getRounds().get(0).getAllUserGameStatuses().get(1L).getIsReady());


        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/game/1"), messageCaptor.capture());

        List<Message> capturedMessages = messageCaptor.getAllValues();

        boolean hasSate = capturedMessages.stream().anyMatch(message -> message.getType() == MessageType.GAME_STATE);
        assertTrue(hasSate);

    }
    
    @Test
    void processGuessMessage_noGuessAllowed() throws Exception {
        Lobby lobby = getLobby();

        Train mockTrain = new Train();
        mockTrain.setCurrentX(100L);
        mockTrain.setCurrentY(100L);
        mockTrain.setLineOrigin(new Station("Start", 0L, 0L, 0, 0));
        mockTrain.setLineDestination(new Station("End", 200L, 200L, 10, 10));

        when(trainPositionFetcher.fetchTrainsMock(anyInt())).thenReturn(List.of(mockTrain));
        when(trainPositionFetcher.fetchTrainsLive(anyInt())).thenReturn(List.of(mockTrain));
        Game game = gameService.setupGame(lobby);

        GuessMessageDTO guessMessage = new GuessMessageDTO(1L, 1L);
        guessMessage.setXcoordinate(100L); guessMessage.setYcoordinate(100L);

        Mockito.clearInvocations(messagingTemplate);
        gameService.processGuessMessage(guessMessage, lobby);


       assertNull(game.getRounds().get(0).getScores().get(1L));

       verify(messagingTemplate, never()).convertAndSend(anyString(), any(Message.class));
    }

    @Test
    void readyForNextRound() {
    }

    @Test
    void updateUserGameStatus() {
    }

    @Test
    void canSubmitGuess() {
    }

    @Test
    void roundStart() {
    }

    @Test
    void roundEnd() {
    }

    @Test
    void allowedToPublish() {
    }

    @Test
    void publishScores() {
    }

    @Test
    void calculateScore() {
    }

    @Test
    void calculateGuessDistance() {
    }

    @Test
    void gameTearDown() {
    }
}