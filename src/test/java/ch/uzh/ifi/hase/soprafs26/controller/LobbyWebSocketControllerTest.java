package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.MessageType;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.security.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import ch.uzh.ifi.hase.soprafs26.websocket.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        lobbyWebSocketController = new LobbyWebSocketController(
                lobbyService, authService);
    }

    // =========================================================
    // @MessageMapping("/lobby/{lobbyId}/start")
    // =========================================================

    @Test
    void startGameAdmin_validAdmin_startsGame() {
        User admin = new User();
        admin.setUserId(1L);
        admin.setToken("valid-token");

        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);
        lobby.setAdmin(admin);

        when(lobbyService.getLobbyById(anyLong())).thenReturn(lobby);
        when(authService.authUser(any())).thenReturn(true);

        lobbyWebSocketController.startGameAdmin("1", "1", "valid-token");

        verify(lobbyService, times(1)).startGame(1L);
    }

    @Test
    void startGameAdmin_invalidToken_throwsUnauthorized() {
        User admin = new User();
        admin.setUserId(1L);
        admin.setToken("valid-token");

        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);
        lobby.setAdmin(admin);

        when(authService.authUser(any())).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyWebSocketController.startGameAdmin("1", "1", "wrong-token"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(lobbyService, never()).startGame(any());
    }

    @Test
    void startGameAdmin_nonAdminUser_throwsForbidden() {
        User admin = new User();
        admin.setUserId(1L);
        admin.setToken("admin-token");

        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);
        lobby.setAdmin(admin); // Admin is user 1

        when(lobbyService.getLobbyById(anyLong())).thenReturn(lobby);
        when(authService.authUser(any())).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyWebSocketController.startGameAdmin("1", "2", "user-token"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(lobbyService, never()).startGame(any());
    }


    // =========================================================
    // @MessageMapping("/lobby/{lobbyId}/leave")
    // =========================================================

    @Test
    void leaveLobby_validLeaveLobbyMessage() {
        when(authService.authUser(any())).thenReturn(true);

        lobbyWebSocketController.leaveLobby("1", "1", "user-token");

        verify(lobbyService, times(1)).leaveLobby(eq(1L), eq(1L));
    }

    @Test
    void leaveLobby_notAuthenticated() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);

        when(authService.authUser(any())).thenReturn(false);

        lobbyWebSocketController.leaveLobby("1", "1", "wrong-token");

        verify(lobbyService, never()).leaveLobby(any(), any());
    }

    @Test
    void leaveLobby_authThrowsException_exceptionIsSilenced() {
        // The controller catches all ResponseStatusExceptions inside leaveLobby —
        // the caller must never see them.
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);

        when(authService.authUser(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        assertDoesNotThrow(() -> lobbyWebSocketController.leaveLobby("1", "2", "wrong-token"));

        verify(lobbyService, never()).leaveLobby(any(), any());
    }

}
