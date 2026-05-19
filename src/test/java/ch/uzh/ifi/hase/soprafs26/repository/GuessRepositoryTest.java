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
public class GuessRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GuessRepository guessRepository;

    // ═══════════════════════════════════════════════════════════════════
    // findByRoundAndUserUserId
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: Der Guess eines bestimmten Users in einer bestimmten Runde wird gefunden.
     * Faengt Bug: Wenn der Pfad "User_UserId" im abgeleiteten Query falsch ist,
     * wuerde GameService immer null zurueckbekommen und einen neuen Guess anlegen,
     * obwohl schon einer existiert — Duplicate-Constraint-Violation.
     */
    @Test
    public void findByRoundAndUserUserId_existingGuess_returnsGuess() {
        User user = persistUser("guesser", "guesser@uzh.ch");
        Round round = persistRound(1);

        Guess guess = persistGuess(round, user, 47.0f, 8.0f, 100, 1.5f);

        Guess found = guessRepository.findByRoundAndUserUserId(round, user.getUserId());

        assertNotNull(found);
        assertEquals(guess.getId(), found.getId());
        assertEquals(47.0f, found.getLat(), 0.001f);
        assertEquals(8.0f,  found.getLon(), 0.001f);
    }

    /**
     * Prueft: Wenn der User noch keinen Guess fuer diese Runde abgegeben hat,
     * wird null zurueckgegeben.
     * Faengt Bug: GameService prueft auf null, um zu entscheiden ob ein neuer
     * Guess angelegt werden soll — false positive wuerde Doppel-Inserts erzeugen.
     */
    @Test
    public void findByRoundAndUserUserId_noGuessForUser_returnsNull() {
        User user = persistUser("noguess", "noguess@uzh.ch");
        Round round = persistRound(1);

        Guess found = guessRepository.findByRoundAndUserUserId(round, user.getUserId());

        assertNull(found);
    }

    /**
     * Prueft: Guesses verschiedener User in derselben Runde werden korrekt isoliert.
     * Faengt Bug: Wenn der userId-Filter im Query fehlt, koennte der Guess eines
     * anderen Users zurueckgegeben werden — falsche Punkte wuerden zugewiesen.
     */
    @Test
    public void findByRoundAndUserUserId_multipleUsersInRound_returnsCorrectGuess() {
        User userA = persistUser("userA", "a@uzh.ch");
        User userB = persistUser("userB", "b@uzh.ch");
        Round round = persistRound(1);

        persistGuess(round, userA, 47.0f, 8.0f, 90, 2.0f);
        persistGuess(round, userB, 48.0f, 9.0f, 50, 5.0f);

        Guess foundA = guessRepository.findByRoundAndUserUserId(round, userA.getUserId());
        Guess foundB = guessRepository.findByRoundAndUserUserId(round, userB.getUserId());

        assertEquals(90, foundA.getPoints());
        assertEquals(50, foundB.getPoints());
    }

    // ═══════════════════════════════════════════════════════════════════
    // findByRound
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: Alle Guesses einer Runde werden zurueckgegeben.
     * Faengt Bug: Wenn der Round-Filter fehlt, wuerden Guesses aus anderen
     * Runden eingemischt und die Punkte-Berechnung wuerde falsche Werte liefern.
     */
    @Test
    public void findByRound_returnsAllGuessesForThatRound() {
        User userA = persistUser("fa", "fa@uzh.ch");
        User userB = persistUser("fb", "fb@uzh.ch");
        Round round1 = persistRound(1);
        Round round2 = persistRound(2);

        persistGuess(round1, userA, 47.0f, 8.0f, 80, 1.0f);
        persistGuess(round1, userB, 48.0f, 9.0f, 60, 2.0f);
        persistGuess(round2, userA, 46.0f, 7.0f, 70, 3.0f);

        List<Guess> guesses = guessRepository.findByRound(round1);

        assertEquals(2, guesses.size());
        assertTrue(guesses.stream().allMatch(g -> g.getRound().getRoundId().equals(round1.getRoundId())));
    }

    /**
     * Prueft: Eine Runde ohne Guesses liefert eine leere Liste.
     */
    @Test
    public void findByRound_noGuesses_returnsEmptyList() {
        Round round = persistRound(1);

        List<Guess> guesses = guessRepository.findByRound(round);

        assertTrue(guesses.isEmpty());
    }

    // ═══════════════════════════════════════════════════════════════════
    // deleteByRound
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: deleteByRound loescht genau die Guesses der angegebenen Runde.
     * Faengt Bug: Ohne Round-Filter wuerden alle Guesses geloescht — die naechste
     * Runde haette keine Guess-Daten mehr zum Auswerten.
     */
    @Test
    public void deleteByRound_removesOnlyGuessesOfThatRound() {
        User user = persistUser("deluser", "del@uzh.ch");
        Round round1 = persistRound(1);
        Round round2 = persistRound(2);

        persistGuess(round1, user, 47.0f, 8.0f, 80, 1.0f);
        persistGuess(round2, user, 48.0f, 9.0f, 70, 2.0f);

        guessRepository.deleteByRound(round1);
        entityManager.flush();
        entityManager.clear();

        assertTrue(guessRepository.findByRound(round1).isEmpty(),
                "Guesses for round1 must be deleted");
        assertEquals(1, guessRepository.findByRound(round2).size(),
                "Guesses for round2 must be untouched");
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

    private Round persistRound(int roundNumber) {
        User admin = new User();
        UserProfile p = new UserProfile();
        p.setUsername("admin_r" + roundNumber + "_" + System.nanoTime());
        p.setEmail(p.getUsername() + "@uzh.ch");
        p.setPassword("pw");
        admin.setUserProfile(p);
        admin.setIsOnline(false);
        admin.setIsGuest(false);
        admin.setUserScoreboard(new UserScoreboard());
        entityManager.persist(admin);

        Lobby lobby = new Lobby();
        lobby.setLobbyName("Lobby_r" + roundNumber + "_" + System.nanoTime());
        lobby.setLobbyCode(String.valueOf(roundNumber) + "AAA");
        lobby.setAdmin(admin);
        lobby.setMaxPlayers(4);
        lobby.setMaxRounds(5);
        lobby.setVisibility(LobbyVisibility.PUBLIC);
        lobby.setLobbyState(LobbyState.IN_GAME);
        entityManager.persist(lobby);

        Round round = new Round();
        round.setLobby(lobby);
        round.setRoundNumber(roundNumber);
        round.setTrainData("{}");
        entityManager.persist(round);
        entityManager.flush();
        return round;
    }

    private Guess persistGuess(Round round, User user, float lat, float lon,
                                int points, float distance) {
        Guess guess = new Guess();
        guess.setRound(round);
        guess.setUser(user);
        guess.setLat(lat);
        guess.setLon(lon);
        guess.setPoints(points);
        guess.setDistanceToTrain(distance);
        guess.setHasGuessed(true);
        entityManager.persist(guess);
        entityManager.flush();
        return guess;
    }
}
