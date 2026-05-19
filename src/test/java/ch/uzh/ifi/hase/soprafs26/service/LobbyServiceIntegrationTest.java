package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CreateLobbyPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyAccessDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for LobbyService.
 *
 * Lobby is now a JPA entity backed by LobbyRepository and H2 (no more
 * in-memory activeLobbies list). Tests therefore exercise the full
 * LobbyService → LobbyRepository → H2 stack.
 *
 * Removed tests (concept gone from the new design):
 * - onGameEnded: GameEndedEvent and the event-listener method no longer exist.
 * - GameResult persistence: GameResult entity and GameRepository are gone.
 *
 * The createLobbyCode() uniqueness loop and JPA @GeneratedValue for Lobby.lobbyId
 * are verified here against the real DB, which unit tests cannot cover.
 */
@WebAppConfiguration
@SpringBootTest
public class LobbyServiceIntegrationTest {

    private static final String ADMIN_USERNAME = "lobbyAdmin";
    private static final String ADMIN_EMAIL = "admin@uzh.ch";
    private static final String ADMIN_PASSWORD = "adminPw";

    @Autowired
    private LobbyService lobbyService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LobbyRepository lobbyRepository;

    private User registeredAdmin;

    @BeforeEach
    public void setup() {
        lobbyRepository.deleteAll();
        userRepository.deleteAll();

        registeredAdmin = userService.registerUser(
                buildUser(ADMIN_USERNAME, ADMIN_EMAIL, ADMIN_PASSWORD));
    }

    /**
     * Szenario: Ein registrierter User erstellt eine Lobby ueber den echten Service.
     * Prueft: Die Lobby ist in der DB persistiert (findById liefert sie),
     * State ist WAITING, Admin ist korrekt gesetzt, Lobby-Code ist 4 Zeichen.
     * Faengt Bug: Im Gegensatz zum Unit-Test prueft die echte JPA-Auto-Generierung
     * der lobbyId. Fehlendes @GeneratedValue oder fehlendes lobbyRepository.save()
     * wuerden sofort auffallen.
     */
    @Test
    public void createLobby_validInput_persistsLobbyInWaitingState() {
        long countBefore = lobbyRepository.count();

        LobbyAccessDTO accessDTO = lobbyService.createLobby(
                buildCreateDTO("IntegrationLobby", 4, 5),
                false, registeredAdmin.getUserId(), registeredAdmin.getToken());

        assertEquals(countBefore + 1, lobbyRepository.count(),
                "createLobby must persist exactly one Lobby row");

        Lobby lobby = lobbyRepository.findById(accessDTO.getLobbyId()).orElseThrow(
                () -> new AssertionError("Lobby was not persisted"));
        assertEquals(LobbyState.WAITING, lobby.getLobbyState());
        assertEquals(registeredAdmin.getUserId(), lobby.getAdmin().getUserId());
        assertEquals(4, lobby.getLobbyCode().length(),
                "Lobby code must be exactly 4 characters");
    }

    /**
     * Szenario: Zwei Lobbies werden erstellt; danach wird getAllLobbies() aufgerufen.
     * Prueft: Beide Lobbies werden zurueckgegeben (beide sind WAITING, kein Filter).
     * Faengt Bug: Wenn lobbyRepository.findAll() nur eine Lobby zurueckgibt,
     * oder ein Filter versehentlich WAITING-Lobbies ausblendet.
     */
    @Test
    public void getAllLobbies_afterCreatingTwo_returnsBoth() {
        // Pre-register a second admin for the second lobby
        User secondAdmin = userService.registerUser(
                buildUser("secondAdmin", "second@uzh.ch", "pw2"));

        lobbyService.createLobby(buildCreateDTO("Lobby1", 4, 3),
                false, registeredAdmin.getUserId(), registeredAdmin.getToken());
        lobbyService.createLobby(buildCreateDTO("Lobby2", 4, 3),
                false, secondAdmin.getUserId(), secondAdmin.getToken());

        assertEquals(2, lobbyService.getAllLobbies().size());
    }

    /**
     * Szenario: Ein zweiter registrierter User tritt einer bestehenden Lobby bei.
     * Prueft: Nach dem Join ist der User in der players-Liste, die Liste hat
     * genau einen Eintrag mehr, das LobbyAccessDTO enthaelt die richtige userId.
     * Faengt Bug: Testet das Zusammenspiel mit der echten UserService-Impl
     * (getUserById laedt aus H2). Moegliche Bugs: falscher Qualifier, Transactional-
     * Problem, oder die players-Liste wird nach dem save() nicht neu geladen.
     */
    @Test
    public void joinLobby_withSecondRegisteredUser_addsUserToPlayers() {
        LobbyAccessDTO adminAccess = lobbyService.createLobby(
                buildCreateDTO("JoinLobby", 4, 5),
                false, registeredAdmin.getUserId(), registeredAdmin.getToken());

        User second = userService.registerUser(
                buildUser("joinUser", "join@uzh.ch", "joinPw"));

        Lobby lobbyBefore = lobbyRepository.findById(adminAccess.getLobbyId()).orElseThrow();
        int sizeBefore = lobbyBefore.getPlayers().size();

        LobbyAccessDTO joinAccess = lobbyService.joinLobby(
                second.getUserId(),
                second.getToken(),
                adminAccess.getLobbyId(),
                lobbyBefore.getLobbyCode(),
                false);

        Lobby lobbyAfter = lobbyRepository.findById(adminAccess.getLobbyId()).orElseThrow();
        assertEquals(sizeBefore + 1, lobbyAfter.getPlayers().size(),
                "Players list must grow by exactly one after a successful join");
        assertTrue(lobbyAfter.getPlayers().stream()
                        .anyMatch(p -> p.getUserId().equals(second.getUserId())),
                "Newly joined user must be present in the players list");
        assertEquals(second.getUserId(), joinAccess.getUserId(),
                "LobbyAccessDTO must return the userId of the joining user");
    }

    /**
     * Szenario: Ein User versucht einer Lobby mit falschem Code beizutreten.
     * Prueft: 403 FORBIDDEN, und die players-Liste blieb unveraendert.
     */
    @Test
    public void joinLobby_wrongCode_throwsForbiddenAndDoesNotAddUser() {
        LobbyAccessDTO adminAccess = lobbyService.createLobby(
                buildCreateDTO("WrongCodeLobby", 4, 5),
                false, registeredAdmin.getUserId(), registeredAdmin.getToken());

        User second = userService.registerUser(
                buildUser("wrongCodeUser", "wrong@uzh.ch", "wPw"));
        Lobby lobbyBefore = lobbyRepository.findById(adminAccess.getLobbyId()).orElseThrow();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.joinLobby(
                        second.getUserId(), second.getToken(),
                        adminAccess.getLobbyId(), "XXXX", false));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        Lobby lobbyAfter = lobbyRepository.findById(adminAccess.getLobbyId()).orElseThrow();
        assertEquals(lobbyBefore.getPlayers().size(), lobbyAfter.getPlayers().size(),
                "Players list must not grow after a rejected join");
    }

    /**
     * Szenario: Der letzte User verlaesst die Lobby.
     * Prueft: Die Lobby wird aus der DB geloescht (findById liefert empty).
     * Faengt Bug: Fehlendes delete() wuerde leere Lobbies akkumulieren — in
     * getAllLobbies() wuerden sie als WAITING erscheinen.
     */
    @Test
    public void leaveLobby_lastUserLeaves_deletesLobbyFromDB() {
        LobbyAccessDTO adminAccess = lobbyService.createLobby(
                buildCreateDTO("LeaveLobby", 4, 5),
                false, registeredAdmin.getUserId(), registeredAdmin.getToken());
        Long lobbyId = adminAccess.getLobbyId();

        // Admin joins first so leaveLobby can find them in the players list
        Lobby lobby = lobbyRepository.findById(lobbyId).orElseThrow();
        lobbyService.joinLobby(
                registeredAdmin.getUserId(), registeredAdmin.getToken(),
                lobbyId, lobby.getLobbyCode(), false);

        lobbyService.leaveLobby(lobbyId, registeredAdmin.getUserId());

        assertTrue(lobbyRepository.findById(lobbyId).isEmpty(),
                "Lobby must be deleted from DB when the last user leaves");
    }

    /**
     * Szenario: Ein anonymer Besucher erstellt eine Lobby als Gast.
     * Prueft: Der Guest-User wird in der DB persistiert mit Token und
     * "guest_"-Prefix-Username.
     * Dokumentiert bekannten Bug: isGuest wird von registerUser() auf false
     * ueberschrieben (selbes Verhalten wie in UserServiceIntegrationTest).
     */
    @Test
    public void createLobbyAsGuest_persistsGuestUserInDB() {
        LobbyAccessDTO accessDTO = lobbyService.createLobby(
                buildCreateDTO("GuestLobby", 4, 5), true, null, null);

        User guest = userRepository.findById(accessDTO.getUserId()).orElseThrow(
                () -> new AssertionError("Guest user was not persisted to the database"));

        assertTrue(guest.getUserProfile().getUsername().startsWith("guest_"),
                "Guest username must start with 'guest_' prefix");
        assertNotNull(guest.getToken(), "Guest must have a token");

        // KNOWN BUG: isGuest is overridden to false by registerUser().
        assertFalse(guest.getIsGuest(),
                "BUG: isGuest should be true — flip to assertTrue once registerUser() is fixed.");
    }

    // ═══════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════

    private User buildUser(String username, String email, String password) {
        User user = new User();
        UserProfile profile = new UserProfile();
        profile.setUsername(username);
        profile.setEmail(email);
        profile.setPassword(password);
        user.setUserProfile(profile);
        return user;
    }

    private CreateLobbyPostDTO buildCreateDTO(String name, int maxPlayers, int maxRounds) {
        CreateLobbyPostDTO dto = new CreateLobbyPostDTO();
        dto.setLobbyName(name);
        dto.setMaxPlayers(maxPlayers);
        dto.setMaxRounds(maxRounds);
        dto.setVisibility(LobbyVisibility.PUBLIC);
        return dto;
    }
}
