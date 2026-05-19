package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.Friendship;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs26.entity.UserScoreboard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class FriendshipRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FriendshipRepository friendshipRepository;

    // ═══════════════════════════════════════════════════════════════════
    // findByFriend1AndFriend2
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: Eine Freundschaft kann ueber das exakte Paar (friend1, friend2) gefunden werden.
     * Faengt Bug: Wenn der Query die Reihenfolge ignoriert, wuerden doppelte
     * Freundschaftsanfragen moeglich sein.
     */
    @Test
    public void findByFriend1AndFriend2_existingPair_returnsFriendship() {
        User u1 = persistUser("u1", "u1@uzh.ch");
        User u2 = persistUser("u2", "u2@uzh.ch");
        Friendship fs = persistFriendship(u1, u2, null);

        Optional<Friendship> found = friendshipRepository.findByFriend1AndFriend2(u1, u2);

        assertTrue(found.isPresent());
        assertEquals(fs.getFriendshipId(), found.get().getFriendshipId());
    }

    /**
     * Prueft: Die umgekehrte Reihenfolge (u2, u1) liefert Optional.empty().
     * Dokumentiert, dass die Methode NICHT symmetrisch ist — der Caller muss
     * beide Richtungen selbst pruefen.
     */
    @Test
    public void findByFriend1AndFriend2_reversedOrder_returnsEmpty() {
        User u1 = persistUser("r1", "r1@uzh.ch");
        User u2 = persistUser("r2", "r2@uzh.ch");
        persistFriendship(u1, u2, null);

        Optional<Friendship> found = friendshipRepository.findByFriend1AndFriend2(u2, u1);

        assertTrue(found.isEmpty());
    }

    /**
     * Prueft: Nicht existierendes Paar liefert Optional.empty().
     */
    @Test
    public void findByFriend1AndFriend2_nonexistentPair_returnsEmpty() {
        User u1 = persistUser("ne1", "ne1@uzh.ch");
        User u2 = persistUser("ne2", "ne2@uzh.ch");

        Optional<Friendship> found = friendshipRepository.findByFriend1AndFriend2(u1, u2);

        assertTrue(found.isEmpty());
    }

    // ═══════════════════════════════════════════════════════════════════
    // findByFriendshipId
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: Eine Freundschaft kann ueber ihre ID gefunden werden.
     */
    @Test
    public void findByFriendshipId_existingId_returnsFriendship() {
        User u1 = persistUser("fid1", "fid1@uzh.ch");
        User u2 = persistUser("fid2", "fid2@uzh.ch");
        Friendship fs = persistFriendship(u1, u2, null);

        Optional<Friendship> found = friendshipRepository.findByFriendshipId(fs.getFriendshipId());

        assertTrue(found.isPresent());
        assertEquals(fs.getFriendshipId(), found.get().getFriendshipId());
    }

    /**
     * Prueft: Unbekannte ID liefert Optional.empty().
     */
    @Test
    public void findByFriendshipId_unknownId_returnsEmpty() {
        Optional<Friendship> found = friendshipRepository.findByFriendshipId(9999L);

        assertTrue(found.isEmpty());
    }

    // ═══════════════════════════════════════════════════════════════════
    // findAllByUser (accepted friendships)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: findAllByUser gibt nur akzeptierte Freundschaften zurueck
     * (pendingInvitationReceivedBy IS NULL), egal ob der User friend1 oder friend2 ist.
     * Faengt Bug: Wenn der IS NULL Filter fehlt, wuerden ausstehende Anfragen
     * in der Freundesliste erscheinen — falsche UI.
     */
    @Test
    public void findAllByUser_returnsOnlyAcceptedFriendships() {
        User alice = persistUser("alice", "alice@uzh.ch");
        User bob   = persistUser("bob",   "bob@uzh.ch");
        User carol = persistUser("carol", "carol@uzh.ch");
        User dave  = persistUser("dave",  "dave@uzh.ch");

        // alice ↔ bob: accepted (alice is friend1)
        persistFriendship(alice, bob, null);
        // carol ↔ alice: accepted (alice is friend2)
        persistFriendship(carol, alice, null);
        // alice → dave: pending (alice sent, dave hasn't accepted)
        persistFriendship(alice, dave, dave);

        List<Friendship> result = friendshipRepository.findAllByUser(alice);

        assertEquals(2, result.size(),
                "Only accepted friendships where alice is friend1 or friend2 must be returned");
    }

    // ═══════════════════════════════════════════════════════════════════
    // findAllPendingReceivedByUser
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: findAllPendingReceivedByUser gibt Freundschaftsanfragen zurueck,
     * die an den User gerichtet sind (pendingInvitationReceivedBy = user).
     * Faengt Bug: Wenn dieser Query alle pending Friendships liefert statt nur
     * die adressierten, wuerde ein User fremde Anfragen sehen.
     */
    @Test
    public void findAllPendingReceivedByUser_returnsOnlyIncomingRequests() {
        User alice = persistUser("alice2", "alice2@uzh.ch");
        User bob   = persistUser("bob2",   "bob2@uzh.ch");
        User carol = persistUser("carol2", "carol2@uzh.ch");

        // bob sent to alice (alice must accept)
        persistFriendship(bob, alice, alice);
        // carol sent to alice (alice must accept)
        persistFriendship(carol, alice, alice);
        // alice sent to bob (bob must accept — should NOT appear for alice)
        persistFriendship(alice, bob, bob);

        List<Friendship> received = friendshipRepository.findAllPendingReceivedByUser(alice);

        assertEquals(2, received.size());
        assertTrue(received.stream()
                .allMatch(f -> f.getPendingInvitationReceivedBy().getUserId().equals(alice.getUserId())));
    }

    // ═══════════════════════════════════════════════════════════════════
    // findAllPendingSentByUser
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: findAllPendingSentByUser gibt Anfragen zurueck, die der User
     * gesendet hat und die noch nicht akzeptiert wurden.
     * Faengt Bug: Die JPQL-Bedingung "pendingInvitationReceivedBy != :user
     * AND pendingInvitationReceivedBy IS NOT NULL" ist dreiteilig —
     * ein fehlender Teil wuerde empfangene oder akzeptierte Anfragen einmischen.
     */
    @Test
    public void findAllPendingSentByUser_returnsOnlyOutgoingRequests() {
        User alice = persistUser("alice3", "alice3@uzh.ch");
        User bob   = persistUser("bob3",   "bob3@uzh.ch");
        User carol = persistUser("carol3", "carol3@uzh.ch");
        User dave  = persistUser("dave3",  "dave3@uzh.ch");

        // alice sent to bob (pending)
        persistFriendship(alice, bob, bob);
        // alice sent to carol (pending)
        persistFriendship(alice, carol, carol);
        // dave sent to alice — alice received this, NOT sent
        persistFriendship(dave, alice, alice);
        // alice ↔ dave accepted (after alice accepted)
        persistFriendship(alice, dave, null);

        List<Friendship> sent = friendshipRepository.findAllPendingSentByUser(alice);

        assertEquals(2, sent.size());
        // None of the returned friendships should have alice as the pending receiver
        assertTrue(sent.stream()
                .noneMatch(f -> f.getPendingInvitationReceivedBy().getUserId().equals(alice.getUserId())));
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

    private Friendship persistFriendship(User friend1, User friend2, User pendingFor) {
        Friendship fs = new Friendship();
        fs.setFriend1(friend1);
        fs.setFriend2(friend2);
        fs.setPendingInvitationReceivedBy(pendingFor);
        entityManager.persist(fs);
        entityManager.flush();
        return fs;
    }
}
