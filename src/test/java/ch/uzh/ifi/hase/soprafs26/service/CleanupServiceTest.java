package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CleanupServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LobbyRepository lobbyRepository;

    @InjectMocks
    private CleanupService cleanupService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(cleanupService, "guestCutoffMinutes", 1L);
        ReflectionTestUtils.setField(cleanupService, "lobbyCutoffMinutes", 1L);
    }

    private User buildGuest(String username, LocalDateTime creationDate) {
        User user = new User();
        user.setUserId((long) username.hashCode());
        user.setIsGuest(true);
        user.setCreationDate(creationDate);
        UserProfile profile = new UserProfile();
        profile.setUsername(username);
        user.setUserProfile(profile);
        return user;
    }

    private Lobby buildLobby(LobbyState state, LocalDateTime creationDate) {
        Lobby lobby = new Lobby();
        lobby.setLobbyState(state);
        lobby.setCreationDate(creationDate);
        return lobby;
    }

    @Test
    void deleteOrphanedGuestUsers_oldOrphanedGuest_isDeleted() {
        User guest = buildGuest("guest_old", LocalDateTime.now().minusMinutes(5));
        when(lobbyRepository.findAll()).thenReturn(List.of());
        when(userRepository.findAllGuests()).thenReturn(List.of(guest));

        cleanupService.deleteOrphanedGuestUsers();

        verify(userRepository).delete(guest);
    }

    @Test
    void deleteOrphanedGuestUsers_kingBabaBui_isNotDeleted() {
        User king = buildGuest("KingBabaBui", LocalDateTime.now().minusMinutes(5));
        when(lobbyRepository.findAll()).thenReturn(List.of());
        when(userRepository.findAllGuests()).thenReturn(List.of(king));

        cleanupService.deleteOrphanedGuestUsers();

        verify(userRepository, never()).delete(king);
    }

    @Test
    void deleteOrphanedGuestUsers_guestInActiveLobby_isNotDeleted() {
        User guest = buildGuest("guest_active", LocalDateTime.now().minusMinutes(5));
        guest.setUserId(42L);
        Lobby activeLobby = buildLobby(LobbyState.IN_GAME, LocalDateTime.now().minusMinutes(5));
        activeLobby.getPlayers().add(guest);

        when(lobbyRepository.findAll()).thenReturn(List.of(activeLobby));
        when(userRepository.findAllGuests()).thenReturn(List.of(guest));

        cleanupService.deleteOrphanedGuestUsers();

        verify(userRepository, never()).delete(guest);
    }

    @Test
    void deleteOrphanedGuestUsers_tooYoung_isNotDeleted() {
        User guest = buildGuest("guest_young", LocalDateTime.now());
        when(lobbyRepository.findAll()).thenReturn(List.of());
        when(userRepository.findAllGuests()).thenReturn(List.of(guest));

        cleanupService.deleteOrphanedGuestUsers();

        verify(userRepository, never()).delete(guest);
    }

    @Test
    void deleteOrphanedGuestUsers_guestInFinishedLobby_isDeleted() {
        User guest = buildGuest("guest_finished", LocalDateTime.now().minusMinutes(5));
        Lobby finishedLobby = buildLobby(LobbyState.FINISHED, LocalDateTime.now().minusMinutes(5));
        finishedLobby.getPlayers().add(guest);

        when(lobbyRepository.findAll()).thenReturn(List.of(finishedLobby));
        when(userRepository.findAllGuests()).thenReturn(List.of(guest));

        cleanupService.deleteOrphanedGuestUsers();

        verify(userRepository).delete(guest);
    }

    @Test
    void deleteOrphanedLobbies_oldWaitingLobby_isDeleted() {
        Lobby lobby = buildLobby(LobbyState.WAITING, LocalDateTime.now().minusMinutes(5));
        when(lobbyRepository.findAll()).thenReturn(List.of(lobby));

        cleanupService.deleteOrphanedLobbies();

        verify(lobbyRepository).delete(lobby);
    }

    @Test
    void deleteOrphanedLobbies_activeGameLobby_isNotDeleted() {
        Lobby lobby = buildLobby(LobbyState.IN_GAME, LocalDateTime.now().minusMinutes(5));
        when(lobbyRepository.findAll()).thenReturn(List.of(lobby));

        cleanupService.deleteOrphanedLobbies();

        verify(lobbyRepository, never()).delete(lobby);
    }

    @Test
    void deleteOrphanedLobbies_tooYoung_isNotDeleted() {
        Lobby lobby = buildLobby(LobbyState.WAITING, LocalDateTime.now());
        when(lobbyRepository.findAll()).thenReturn(List.of(lobby));

        cleanupService.deleteOrphanedLobbies();

        verify(lobbyRepository, never()).delete(lobby);
    }
}