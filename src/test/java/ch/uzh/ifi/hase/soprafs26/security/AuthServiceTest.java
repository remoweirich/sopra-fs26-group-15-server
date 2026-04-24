package ch.uzh.ifi.hase.soprafs26.security;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.objects.Lobby;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    LobbyService lobbyService;

    @InjectMocks
    private AuthService authService;

    @Test
    void authUser_success() {
        User user = new User();
        user.setUserId(1L);
        user.setToken("correct-token");
        AuthHeader authHeader = new AuthHeader(1L, "correct-token");

        when(userRepository.findById(authHeader.getUserId())).thenReturn(Optional.of(user));

        // WHEN
        Boolean result = authService.authUser(authHeader);

        // THEN
        assertTrue(result);
    }

    @Test
    void authUser_wrongToken_returnsFalse() {
        User user = new User();
        user.setUserId(1L);
        user.setToken("correct-token");
        AuthHeader authHeader = new AuthHeader(1L, "wrong-token");

        when(userRepository.findById(authHeader.getUserId())).thenReturn(Optional.of(user));

        // WHEN
        Boolean result = authService.authUser(authHeader);

        // THEN
        assertFalse(result);
    }

    @Test
    void authUser_userNotFound_throwsNotFoundException() {

        AuthHeader authHeader = new AuthHeader(1L, "any-token");
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.authUser(authHeader);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void isUserInLobby_userAuthenticatedAndInLobby_returnsTrue() {
        // GIVEN
        Long userId = 1L;
        String token = "valid-token";
        Long lobbyId = 100L;

        User user = new User();
        user.setUserId(userId);
        user.setToken(token);

        // Mock für Auth
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Lobby mockLobby = mock(Lobby.class);
        when(lobbyService.getLobbyById(lobbyId)).thenReturn(mockLobby);
        when(mockLobby.existsUser(userId)).thenReturn(true);

        // WHEN
        Boolean result = authService.isUserInLobby(userId, token, lobbyId);

        // THEN
        assertTrue(result);
    }

    @Test
    void isUserInLobby_authFails_returnsFalse() {
        User user = new User();
        user.setUserId(1L);
        user.setToken("valid-token");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Boolean result = authService.isUserInLobby(1L, "wrong", 100L);

        assertFalse(result);
        verifyNoInteractions(lobbyService);
    }
}