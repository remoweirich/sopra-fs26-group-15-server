package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.MessageType;
import ch.uzh.ifi.hase.soprafs26.objects.Admin;
import ch.uzh.ifi.hase.soprafs26.objects.Lobby;
import ch.uzh.ifi.hase.soprafs26.security.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import ch.uzh.ifi.hase.soprafs26.websocket.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LobbyWebSocketController WebSocket message handlers.
 *
 * LobbyWebSocketController uses com.fasterxml.jackson.databind.ObjectMapper
 * (Jackson 2 / the shaded copy on the classpath) to deserialise message payloads.
 *
 * Correctness is verified by asserting which service methods are called (or not)
 * for a given incoming Message.
 */
@ExtendWith(MockitoExtension.class)
class LobbyWebSocketControllerTest {

    @Mock
    private LobbyService lobbyService;

    @Mock
    private AuthService authService;

    private LobbyWebSocketController lobbyWebSocketController;

    @BeforeEach
    void setUp() {
        // Inject a real ObjectMapper so convertValue() actually deserialises payloads.
        // LobbyWebSocketController uses com.fasterxml.jackson.databind.ObjectMapper.
        lobbyWebSocketController = new LobbyWebSocketController(
                lobbyService, authService, new ObjectMapper());
    }

    // =========================================================
    // @MessageMapping("/lobby/{lobbyId}/start")
    // =========================================================

    @Test
    void startGameAdmin_validAdmin_startsGame() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);
        lobby.setAdmin(new Admin(1L, "valid-token"));

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 1);          // Integer → Jackson widens to Long
        payload.put("token", "valid-token");
        Message message = new Message(MessageType.START_GAME, payload);

        when(lobbyService.getLobbyById(1L)).thenReturn(lobby);
        when(authService.authUser(any())).thenReturn(true);

        lobbyWebSocketController.startGameAdmin("1", message);

        verify(lobbyService, times(1)).startGame(1L);
    }

    @Test
    void startGameAdmin_invalidToken_throwsUnauthorized() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);
        lobby.setAdmin(new Admin(1L, "valid-token"));

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 1);
        payload.put("token", "wrong-token");
        Message message = new Message(MessageType.START_GAME, payload);

        when(lobbyService.getLobbyById(1L)).thenReturn(lobby);
        when(authService.authUser(any())).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyWebSocketController.startGameAdmin("1", message));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(lobbyService, never()).startGame(any());
    }

    @Test
    void startGameAdmin_nonAdminUser_throwsForbidden() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);
        lobby.setAdmin(new Admin(99L, "admin-token")); // Admin is user 99

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 1);           // User 1 is authenticated but not the admin
        payload.put("token", "user-token");
        Message message = new Message(MessageType.START_GAME, payload);

        when(lobbyService.getLobbyById(1L)).thenReturn(lobby);
        when(authService.authUser(any())).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyWebSocketController.startGameAdmin("1", message));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(lobbyService, never()).startGame(any());
    }

    @Test
    void startGameAdmin_lobbyNotFound_exceptionPropagates() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 1);
        payload.put("token", "valid-token");
        Message message = new Message(MessageType.START_GAME, payload);

        when(lobbyService.getLobbyById(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));

        assertThrows(ResponseStatusException.class,
                () -> lobbyWebSocketController.startGameAdmin("99", message));

        verify(lobbyService, never()).startGame(any());
        verify(authService, never()).authUser(any());
    }

    // =========================================================
    // @MessageMapping("/lobby/{lobbyId}/leave")
    // =========================================================

    @Test
    void leaveLobby_validLeaveLobbyMessage_removesUserFromLobby() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 1);
        payload.put("token", "valid-token");
        Message message = new Message(MessageType.LEAVE_LOBBY, payload);

        when(lobbyService.getLobbyById(1L)).thenReturn(lobby);
        when(authService.authUser(any())).thenReturn(true);

        lobbyWebSocketController.leaveLobby("1", message);

        // userId from payload (Integer 1) is widened to Long 1L by Jackson
        verify(lobbyService, times(1)).leaveLobby(eq(1L), eq(1L));
    }

    @Test
    void leaveLobby_notAuthenticated_doesNotLeave() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 1);
        payload.put("token", "wrong-token");
        Message message = new Message(MessageType.LEAVE_LOBBY, payload);

        when(lobbyService.getLobbyById(1L)).thenReturn(lobby);
        when(authService.authUser(any())).thenReturn(false);

        lobbyWebSocketController.leaveLobby("1", message);

        verify(lobbyService, never()).leaveLobby(any(), any());
    }

    @Test
    void leaveLobby_wrongMessageType_doesNotLeave() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);

        // Any type other than LEAVE_LOBBY is ignored
        Message message = new Message(MessageType.LOBBY_STATE, new HashMap<>());

        when(lobbyService.getLobbyById(1L)).thenReturn(lobby);

        lobbyWebSocketController.leaveLobby("1", message);

        verify(lobbyService, never()).leaveLobby(any(), any());
        verify(authService, never()).authUser(any());
    }

    @Test
    void leaveLobby_authThrowsException_exceptionIsSilenced() {
        // The controller catches all ResponseStatusExceptions inside leaveLobby —
        // the caller must never see them.
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 1);
        payload.put("token", "some-token");
        Message message = new Message(MessageType.LEAVE_LOBBY, payload);

        when(lobbyService.getLobbyById(1L)).thenReturn(lobby);
        when(authService.authUser(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        assertDoesNotThrow(() -> lobbyWebSocketController.leaveLobby("1", message));

        verify(lobbyService, never()).leaveLobby(any(), any());
    }

    @Test
    void leaveLobby_lobbyNotFound_exceptionPropagates() {
        // getLobbyById throws before the type-check, so the exception is NOT caught
        // (the try/catch is inside the LEAVE_LOBBY branch, after the lobby is loaded)
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 1);
        payload.put("token", "valid-token");
        Message message = new Message(MessageType.LEAVE_LOBBY, payload);

        when(lobbyService.getLobbyById(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));

        assertThrows(ResponseStatusException.class,
                () -> lobbyWebSocketController.leaveLobby("99", message));
    }
}
