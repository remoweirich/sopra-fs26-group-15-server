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
public class RoundRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RoundRepository roundRepository;

    // ═══════════════════════════════════════════════════════════════════
    // findByLobbyOrderByRoundNumberAsc
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: Runden einer Lobby werden in aufsteigender Reihenfolge zurueckgegeben.
     * Faengt Bug: Ohne "OrderByRoundNumberAsc" wuerde die Reihenfolge nicht
     * garantiert sein — GameService wuerde die falsche Runde laden.
     */
    @Test
    public void findByLobbyOrderByRoundNumberAsc_returnsRoundsInOrder() {
        Lobby lobby = persistLobby("OrderLobby");

        Round r3 = persistRound(lobby, 3, "data3");
        Round r1 = persistRound(lobby, 1, "data1");
        Round r2 = persistRound(lobby, 2, "data2");

        List<Round> rounds = roundRepository.findByLobbyOrderByRoundNumberAsc(lobby);

        assertEquals(3, rounds.size());
        assertEquals(1, rounds.get(0).getRoundNumber());
        assertEquals(2, rounds.get(1).getRoundNumber());
        assertEquals(3, rounds.get(2).getRoundNumber());
    }

    /**
     * Prueft: Runden einer anderen Lobby erscheinen nicht im Ergebnis.
     * Faengt Bug: Wenn der Lobby-Filter fehlen wuerde, wuerden Runden anderer
     * Lobbies vermischt und ein falscher Train geladen.
     */
    @Test
    public void findByLobbyOrderByRoundNumberAsc_onlyReturnsRoundsForThatLobby() {
        Lobby lobbyA = persistLobby("LobbyA");
        Lobby lobbyB = persistLobby("LobbyB");

        persistRound(lobbyA, 1, "trainA");
        persistRound(lobbyB, 1, "trainB");
        persistRound(lobbyB, 2, "trainB2");

        List<Round> rounds = roundRepository.findByLobbyOrderByRoundNumberAsc(lobbyA);

        assertEquals(1, rounds.size());
        assertEquals("trainA", rounds.get(0).getTrainData());
    }

    /**
     * Prueft: Leere Lobby (keine Runden) liefert eine leere Liste.
     */
    @Test
    public void findByLobbyOrderByRoundNumberAsc_noRounds_returnsEmptyList() {
        Lobby lobby = persistLobby("EmptyLobby");

        List<Round> rounds = roundRepository.findByLobbyOrderByRoundNumberAsc(lobby);

        assertTrue(rounds.isEmpty());
    }

    // ═══════════════════════════════════════════════════════════════════
    // deleteByLobby
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: deleteByLobby entfernt genau die Runden der angegebenen Lobby.
     * Faengt Bug: Wenn der Filter fehlt, wuerden Runden aller Lobbies geloescht —
     * ein katastrophaler Datenverlust beim Lobby-Teardown.
     */
    @Test
    public void deleteByLobby_removesOnlyRoundsOfThatLobby() {
        Lobby lobbyA = persistLobby("DeleteLobbyA");
        Lobby lobbyB = persistLobby("DeleteLobbyB");

        persistRound(lobbyA, 1, "a1");
        persistRound(lobbyA, 2, "a2");
        persistRound(lobbyB, 1, "b1");

        roundRepository.deleteByLobby(lobbyA);
        entityManager.flush();
        entityManager.clear();

        assertEquals(0, roundRepository.findByLobbyOrderByRoundNumberAsc(lobbyA).size(),
                "All rounds of lobbyA must be deleted");
        assertEquals(1, roundRepository.findByLobbyOrderByRoundNumberAsc(lobbyB).size(),
                "Rounds of lobbyB must be untouched");
    }

    // ═══════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════

    private Lobby persistLobby(String name) {
        User admin = new User();
        UserProfile profile = new UserProfile();
        profile.setUsername(name + "_admin");
        profile.setEmail(name + "@uzh.ch");
        profile.setPassword("pw");
        admin.setUserProfile(profile);
        admin.setIsOnline(false);
        admin.setIsGuest(false);
        admin.setUserScoreboard(new UserScoreboard());
        entityManager.persist(admin);

        Lobby lobby = new Lobby();
        lobby.setLobbyName(name);
        lobby.setLobbyCode(name.substring(0, Math.min(4, name.length())).toUpperCase());
        lobby.setAdmin(admin);
        lobby.setMaxPlayers(4);
        lobby.setMaxRounds(5);
        lobby.setVisibility(LobbyVisibility.PUBLIC);
        lobby.setLobbyState(LobbyState.IN_GAME);
        entityManager.persist(lobby);
        entityManager.flush();
        return lobby;
    }

    private Round persistRound(Lobby lobby, int roundNumber, String trainData) {
        Round round = new Round();
        round.setLobby(lobby);
        round.setRoundNumber(roundNumber);
        round.setTrainData(trainData);
        entityManager.persist(round);
        entityManager.flush();
        return round;
    }
}
