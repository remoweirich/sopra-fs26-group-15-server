package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.MessageType;
import ch.uzh.ifi.hase.soprafs26.entity.Guess;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.objects.UserGameStatus;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessMessageDTO;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import ch.uzh.ifi.hase.soprafs26.websocket.Message;
import net.minidev.json.JSONValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
        gameController = new GameController(gameService, lobbyService);
    }

    // =========================================================
    // @MessageMapping("/game/{gameId}/guess")
    // =========================================================

    @Test
    void processGuessMessage_guessMessageType_forwardsToGameService() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);

        GuessMessageDTO guessMessageDTO = new GuessMessageDTO();
        guessMessageDTO.setLobbyId(1L);
        guessMessageDTO.setUserId(2L);
        guessMessageDTO.setXCoordinate(100L);
        guessMessageDTO.setYCoordinate(200L);

        when(lobbyService.getLobbyById(1L)).thenReturn(lobby);

        gameController.processGuessMessage(1L, "2", "valid-token", guessMessageDTO);

        // Payload was converted and forwarded to the game service
        verify(gameService, times(1)).processGuessMessage(any(GuessMessageDTO.class), eq(lobby));
    }


    @Test
    void processGuessMessage_lobbyNotFound_exceptionPropagates() {
        GuessMessageDTO guessMessageDTO = new GuessMessageDTO();

        when(lobbyService.getLobbyById(anyLong()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));

        assertThrows(ResponseStatusException.class,
                () -> gameController.processGuessMessage(99L, "2", "valid-token", guessMessageDTO), "Does not throw exception when no lobby is found");

        verify(gameService, never()).processGuessMessage(any(), any());
    }

    // =========================================================
    // @MessageMapping("/game/{gameId}/ready")
    // =========================================================

    @Test
    void readyForNextRound_readyMessageType_forwardsToGameService() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);

        when(lobbyService.getLobbyById(1L)).thenReturn(lobby);

        gameController.readyForNextRound(1L, "1", "valid-token");

        verify(gameService, times(1)).readyForNextRound(any(UserGameStatus.class), eq(lobby));
    }


    @Test
    void readyForNextRound_lobbyNotFound_exceptionPropagates() {

        when(lobbyService.getLobbyById(anyLong()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));


        assertThrows(ResponseStatusException.class,
                () -> gameController.readyForNextRound(99L, "1", "valid-token"), "Does not throw exception when no lobby is found");

        verify(gameService, never()).readyForNextRound(any(), any());
    }
}
