package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.MessageType;
import ch.uzh.ifi.hase.soprafs26.objects.Lobby;
import ch.uzh.ifi.hase.soprafs26.objects.UserGameStatus;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessMessageDTO;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import ch.uzh.ifi.hase.soprafs26.websocket.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GameController WebSocket message handlers.
 *
 * The controller has no HTTP response body — correctness is verified by
 * asserting that the right service methods are (or are not) called for a
 * given incoming Message.
 */
@ExtendWith(MockitoExtension.class)
class GameControllerTest {

    @Mock
    private GameService gameService;

    @Mock
    private LobbyService lobbyService;

    private GameController gameController;

    @BeforeEach
    void setUp() {
        // Inject a real ObjectMapper so convertValue() actually deserialises payloads.
        // GameController uses tools.jackson.databind.ObjectMapper.
        gameController = new GameController(gameService, new ObjectMapper(), lobbyService);
    }

    // =========================================================
    // @MessageMapping("/game/{gameId}/guess")
    // =========================================================

    @Test
    void processGuessMessage_guessMessageType_forwardsToGameService() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);

        Map<String, Object> payload = new HashMap<>();
        payload.put("lobbyId", 1);
        payload.put("userId", 2);
        payload.put("Xcoordinate", 100);
        payload.put("Ycoordinate", 200);
        Message message = new Message(MessageType.GUESS_MESSAGE, payload);

        when(lobbyService.getLobbyById(1L)).thenReturn(lobby);

        gameController.processGuessMessage(1L, message);

        // Payload was converted and forwarded to the game service
        verify(gameService, times(1)).processGuessMessage(any(GuessMessageDTO.class), eq(lobby));
    }

    @Test
    void processGuessMessage_wrongMessageType_gameServiceNotCalled() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);

        // Any type that is not GUESS_MESSAGE should be silently ignored
        Message message = new Message(MessageType.LOBBY_STATE, new HashMap<>());

        when(lobbyService.getLobbyById(1L)).thenReturn(lobby);

        gameController.processGuessMessage(1L, message);

        verify(gameService, never()).processGuessMessage(any(), any());
    }

    @Test
    void processGuessMessage_lobbyNotFound_exceptionPropagates() {
        Message message = new Message(MessageType.GUESS_MESSAGE, new HashMap<>());

        when(lobbyService.getLobbyById(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));

        assertThrows(ResponseStatusException.class,
                () -> gameController.processGuessMessage(99L, message));

        verify(gameService, never()).processGuessMessage(any(), any());
    }

    // =========================================================
    // @MessageMapping("/game/{gameId}/ready")
    // =========================================================

    @Test
    void readyForNextRound_readyMessageType_forwardsToGameService() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 1);
        payload.put("isReady", true);
        Message message = new Message(MessageType.READY_FOR_NEXT_ROUND, payload);

        when(lobbyService.getLobbyById(1L)).thenReturn(lobby);

        gameController.readyForNextRound(1L, message);

        verify(gameService, times(1)).readyForNextRound(any(UserGameStatus.class), eq(lobby));
    }

    @Test
    void readyForNextRound_wrongMessageType_gameServiceNotCalled() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);

        // GUESS_MESSAGE is not READY_FOR_NEXT_ROUND — should be silently ignored
        Message message = new Message(MessageType.GUESS_MESSAGE, new HashMap<>());

        when(lobbyService.getLobbyById(1L)).thenReturn(lobby);

        gameController.readyForNextRound(1L, message);

        verify(gameService, never()).readyForNextRound(any(), any());
    }

    @Test
    void readyForNextRound_lobbyNotFound_exceptionPropagates() {
        Message message = new Message(MessageType.READY_FOR_NEXT_ROUND, new HashMap<>());

        when(lobbyService.getLobbyById(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));

        assertThrows(ResponseStatusException.class,
                () -> gameController.readyForNextRound(99L, message));

        verify(gameService, never()).readyForNextRound(any(), any());
    }
}
