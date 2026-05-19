package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class RoundHistoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RoundHistoryRepository roundHistoryRepository;

    // ═══════════════════════════════════════════════════════════════════
    // findByUserUserId
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: findByUserUserId gibt alle Runden-Historien eines Users zurueck.
     * Faengt Bug: Wenn der Pfad "User_UserId" falsch ist, wuerde die
     * Achievements- und Scoreboard-Berechnung keinen Input erhalten.
     */
    @Test
    public void findByUserUserId_returnsHistoriesForThatUser() {
        User userA = persistUser("playerA", "a@uzh.ch");
        User userB = persistUser("playerB", "b@uzh.ch");
        Lobby lobby = persistLobby("HistoryLobby", userA);

        persistRoundHistory(lobby, userA, 1, 80, 1.5f);
        persistRoundHistory(lobby, userA, 2, 60, 2.0f);
        persistRoundHistory(lobby, userB, 1, 50, 3.0f);

        List<RoundHistory> result = roundHistoryRepository.findByUserUserId(userA.getUserId());

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(rh -> rh.getUser().getUserId().equals(userA.getUserId())));
    }

    /**
     * Prueft: Unbekannte userId liefert leere Liste (kein Crash).
     */
    @Test
    public void findByUserUserId_unknownUser_returnsEmptyList() {
        List<RoundHistory> result = roundHistoryRepository.findByUserUserId(9999L);

        assertTrue(result.isEmpty());
    }

    // ═══════════════════════════════════════════════════════════════════
    // findByLobbyLobbyId
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: findByLobbyLobbyId gibt alle Historien einer Lobby zurueck.
     * Faengt Bug: LobbyService.getGameResult() baut das Leaderboard komplett
     * aus diesem Query auf — ein falscher Filter wuerde Runden fremder Lobbies
     * einmischen oder eigene Runden weglassen.
     */
    @Test
    public void findByLobbyLobbyId_returnsAllHistoriesForThatLobby() {
        User admin = persistUser("admin", "admin@uzh.ch");
        User player = persistUser("player", "player@uzh.ch");
        Lobby lobbyA = persistLobby("LobbyA", admin);
        Lobby lobbyB = persistLobby("LobbyB", admin);

        persistRoundHistory(lobbyA, admin,  1, 100, 0.5f);
        persistRoundHistory(lobbyA, player, 1, 70,  1.2f);
        persistRoundHistory(lobbyB, admin,  1, 90,  0.8f);

        List<RoundHistory> result = roundHistoryRepository.findByLobbyLobbyId(lobbyA.getLobbyId());

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(rh -> rh.getLobby().getLobbyId().equals(lobbyA.getLobbyId())));
    }

    /**
     * Prueft: Unbekannte lobbyId liefert leere Liste.
     */
    @Test
    public void findByLobbyLobbyId_unknownLobby_returnsEmptyList() {
        List<RoundHistory> result = roundHistoryRepository.findByLobbyLobbyId(9999L);

        assertTrue(result.isEmpty());
    }

    /**
     * Prueft: Die Felder points und distanceToTrain werden korrekt gespeichert
     * und koennen zurueckgelesen werden.
     * Faengt Bug: Ein nicht-nullable Feld ohne Default wuerde beim Persist crashen;
     * ein falscher Typ wuerde Rundungsfehler im Leaderboard erzeugen.
     */
    @Test
    public void findByLobbyLobbyId_correctlyPersistsPointsAndDistance() {
        User admin = persistUser("scorer", "scorer@uzh.ch");
        Lobby lobby = persistLobby("ScoreLobby", admin);

        persistRoundHistory(lobby, admin, 1, 42, 3.14f);

        RoundHistory rh = roundHistoryRepository.findByLobbyLobbyId(lobby.getLobbyId()).get(0);

        assertEquals(42, rh.getPoints());
        assertEquals(3.14f, rh.getDistanceToTrain(), 0.001f);
        assertEquals(1, rh.getRoundNumber());
    }

    // ═══════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════

    private User persistUser(String username, String email) {
        User user = new User();
        UserProfile profile = new UserProfile();
        profile.setUsername(username);
        profile.setEmail(email);
        profile.setPassword("pw");
        user.setUserProfile(profile);
        user.setIsOnline(false);
        user.setIsGuest(false);
        user.setUserScoreboard(new UserScoreboard());
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    private Lobby persistLobby(String name, User admin) {
        Lobby lobby = new Lobby();
        lobby.setLobbyName(name);
        lobby.setLobbyCode(name.substring(0, Math.min(4, name.length())).toUpperCase());
        lobby.setAdmin(admin);
        lobby.setMaxPlayers(4);
        lobby.setMaxRounds(3);
        lobby.setVisibility(LobbyVisibility.PUBLIC);
        lobby.setLobbyState(LobbyState.FINISHED);
        entityManager.persist(lobby);
        entityManager.flush();
        return lobby;
    }

    private void persistRoundHistory(Lobby lobby, User user, int roundNumber,
                                     int points, float distance) {
        RoundHistory rh = new RoundHistory();
        rh.setLobby(lobby);
        rh.setUser(user);
        rh.setRoundNumber(roundNumber);
        rh.setPoints(points);
        rh.setDistanceToTrain(distance);
        entityManager.persist(rh);
        entityManager.flush();
    }
}
