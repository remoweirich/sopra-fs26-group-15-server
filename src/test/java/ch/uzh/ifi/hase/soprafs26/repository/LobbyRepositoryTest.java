package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs26.entity.UserScoreboard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class LobbyRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LobbyRepository lobbyRepository;

    // ═══════════════════════════════════════════════════════════════════
    // findByLobbyCode
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: Eine Lobby kann ueber ihren Code gefunden werden.
     * Faengt Bug: Wenn der abgeleitete Query den falschen Feldnamen verwendet
     * (z.B. "code" statt "lobbyCode"), wuerde joinLobbyByCode() immer 404 werfen.
     */
    @Test
    public void findByLobbyCode_existingCode_returnsLobby() {
        User admin = persistUser("admin1", "admin1@uzh.ch");
        Lobby lobby = persistLobby("TestLobby", "ABCD", admin);

        Optional<Lobby> found = lobbyRepository.findByLobbyCode("ABCD");

        assertTrue(found.isPresent());
        assertEquals(lobby.getLobbyId(), found.get().getLobbyId());
        assertEquals("TestLobby", found.get().getLobbyName());
    }

    /**
     * Prueft: Unbekannter Code liefert Optional.empty().
     * Faengt Bug: Wenn die Methode null statt Optional.empty() zurueckgibt,
     * wuerden orElseThrow()-Aufrufer mit NPE statt NoSuchElementException crashen.
     */
    @Test
    public void findByLobbyCode_unknownCode_returnsEmpty() {
        Optional<Lobby> found = lobbyRepository.findByLobbyCode("ZZZZ");

        assertTrue(found.isEmpty());
    }

    // ═══════════════════════════════════════════════════════════════════
    // existsByLobbyCode
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: existsByLobbyCode gibt true zurueck wenn der Code bereits vergeben ist.
     * Faengt Bug: Wenn dieser Check immer false zurueckgeben wuerde, koennten
     * doppelte Codes vergeben werden und das falsche Spiel wuerde gejoint.
     */
    @Test
    public void existsByLobbyCode_existingCode_returnsTrue() {
        User admin = persistUser("admin2", "admin2@uzh.ch");
        persistLobby("Lobby2", "BCDE", admin);

        assertTrue(lobbyRepository.existsByLobbyCode("BCDE"));
    }

    /**
     * Prueft: existsByLobbyCode gibt false zurueck wenn der Code noch frei ist.
     * Faengt Bug: Wenn immer true zurueckgegeben wuerde, wuerde die Code-
     * Generierungsschleife in createLobbyCode() unendlich laufen.
     */
    @Test
    public void existsByLobbyCode_unknownCode_returnsFalse() {
        assertFalse(lobbyRepository.existsByLobbyCode("XXXX"));
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
        return user;
    }

    private Lobby persistLobby(String name, String code, User admin) {
        Lobby lobby = new Lobby();
        lobby.setLobbyName(name);
        lobby.setLobbyCode(code);
        lobby.setAdmin(admin);
        lobby.setMaxPlayers(4);
        lobby.setMaxRounds(5);
        lobby.setVisibility(LobbyVisibility.PUBLIC);
        lobby.setLobbyState(LobbyState.WAITING);
        entityManager.persist(lobby);
        entityManager.flush();
        return lobby;
    }
}
