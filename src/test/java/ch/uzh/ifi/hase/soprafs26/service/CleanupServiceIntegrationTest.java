package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs26.entity.UserScoreboard;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@WebAppConfiguration
@SpringBootTest
public class CleanupServiceIntegrationTest {

    @Autowired
    private CleanupService cleanupService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LobbyRepository lobbyRepository;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setup() {
        lobbyRepository.deleteAll();
        userRepository.deleteAll();
        ReflectionTestUtils.setField(cleanupService, "guestCutoffMinutes", 1L);
        ReflectionTestUtils.setField(cleanupService, "lobbyCutoffMinutes", 1L);
    }

    private User buildGuest(String username, LocalDateTime creationDate) {
        User user = new User();
        user.setIsGuest(true);
        user.setIsOnline(false);  // ← hinzufügen
        user.setCreationDate(creationDate);
        user.setToken("token-" + username);
        UserProfile profile = new UserProfile();
        profile.setUsername(username);
        profile.setPassword("pw");
        profile.setEmail(username + "@test.com");
        user.setUserProfile(profile);
        user.setUserScoreboard(new UserScoreboard());
        return userRepository.save(user);
    }

    private Lobby buildLobby(LobbyState state, LocalDateTime creationDate, User admin) {
        Lobby lobby = new Lobby();
        lobby.setLobbyName("TestLobby");
        lobby.setLobbyCode("TEST");
        lobby.setLobbyState(state);
        lobby.setCreationDate(creationDate);
        lobby.setMaxPlayers(4);
        lobby.setMaxRounds(3);
        lobby.setVisibility(LobbyVisibility.PUBLIC);
        lobby.setAdmin(admin);
        lobby.setCurrentRound(0);
        lobby.setPlayers(new ArrayList<>());
        return lobbyRepository.save(lobby);
    }

    @Test
    void deleteOrphanedGuestUsers_oldOrphanedGuest_isDeletedFromDB() {
        User guest = buildGuest("guest_old", LocalDateTime.now().minusMinutes(5));
        ReflectionTestUtils.setField(cleanupService, "guestCutoffMinutes", 0L);

        cleanupService.deleteOrphanedGuestUsers();

        assertTrue(userRepository.findById(guest.getUserId()).isEmpty());
    }

    @Test
    void deleteOrphanedGuestUsers_kingBabaBui_isNotDeleted() {
        User king = buildGuest("KingBabaBui", LocalDateTime.now().minusMinutes(5));

        cleanupService.deleteOrphanedGuestUsers();

        assertTrue(userRepository.findById(king.getUserId()).isPresent());
    }

    @Test
    void deleteOrphanedGuestUsers_guestInActiveLobby_isNotDeleted() {
        User admin = buildGuest("guest_admin", LocalDateTime.now().minusMinutes(5));
        User guest = buildGuest("guest_active", LocalDateTime.now().minusMinutes(5));
        Lobby lobby = buildLobby(LobbyState.IN_GAME, LocalDateTime.now().minusMinutes(5), admin);
        lobby.getPlayers().add(guest);
        lobbyRepository.save(lobby);

        cleanupService.deleteOrphanedGuestUsers();

        assertTrue(userRepository.findById(guest.getUserId()).isPresent());
    }

    @Test
    void deleteOrphanedLobbies_oldWaitingLobby_isDeletedFromDB() {
        User admin = buildGuest("guest_admin2", LocalDateTime.now().minusMinutes(5));
        Lobby lobby = buildLobby(LobbyState.WAITING, LocalDateTime.now().minusMinutes(5), admin);

        ReflectionTestUtils.setField(cleanupService, "lobbyCutoffMinutes", 0L);
        cleanupService.deleteOrphanedLobbies();

        assertTrue(lobbyRepository.findById(lobby.getLobbyId()).isEmpty());
    }

    @Test
    void deleteOrphanedLobbies_activeGameLobby_isNotDeleted() {
        User admin = buildGuest("guest_admin3", LocalDateTime.now().minusMinutes(5));
        Lobby lobby = buildLobby(LobbyState.IN_GAME, LocalDateTime.now().minusMinutes(5), admin);

        cleanupService.deleteOrphanedLobbies();

        assertTrue(lobbyRepository.findById(lobby.getLobbyId()).isPresent());
    }
}