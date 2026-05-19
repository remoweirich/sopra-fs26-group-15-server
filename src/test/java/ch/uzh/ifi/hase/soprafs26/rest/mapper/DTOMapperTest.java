package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DTOMapperTest
 *
 * Verifies that every mapping declared in DTOMapper correctly transfers
 * values between the internal entity model and the external API representation.
 *
 * User data is now split: username/email/password/bio live inside the
 * embedded UserProfile. All tests build entities with a populated UserProfile
 * and assert that mapper-generated DTOs contain the right projected values.
 *
 * Each test follows the pattern: build source → map → assert target.
 * Ignored fields are explicitly checked to stay null so a future @Mapping
 * addition doesn't silently start leaking data (e.g. passwords in UserDTO).
 */
class DTOMapperTest {

    // ═══════════════════════════════════════════════════════════════════
    // convertRegisterPostDTOtoUser
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: RegisterPostDTO-Felder landen korrekt im eingebetteten UserProfile.
     * Faengt Bug: Wenn die @Mapping-Pfade (z.B. "userProfile.username") falsch
     * oder vergessen sind, kommen null-Felder im UserProfile an.
     */
    @Test
    void convertRegisterPostDTOtoUser_mapsIntoUserProfile() {
        RegisterPostDTO dto = new RegisterPostDTO();
        dto.setUsername("alice");
        dto.setEmail("alice@uzh.ch");
        dto.setPassword("secret");
        dto.setUserBio("I love trains");

        User user = DTOMapper.INSTANCE.convertRegisterPostDTOtoUser(dto);

        assertNotNull(user.getUserProfile());
        assertEquals("alice",         user.getUserProfile().getUsername());
        assertEquals("alice@uzh.ch",  user.getUserProfile().getEmail());
        assertEquals("secret",        user.getUserProfile().getPassword());
        assertEquals("I love trains", user.getUserProfile().getUserBio());
    }

    /**
     * Prueft: Felder, die laut @Mapping ignoriert werden, sind null.
     * Faengt Bug: Wenn jemand eine ignore-Annotation entfernt, wuerde z.B.
     * ein zufaelliger Token oder eine userId ins DTO eingeschleust.
     */
    @Test
    void convertRegisterPostDTOtoUser_ignoredFieldsAreNull() {
        RegisterPostDTO dto = new RegisterPostDTO();
        dto.setUsername("bob");
        dto.setEmail("bob@uzh.ch");
        dto.setPassword("pw");

        User user = DTOMapper.INSTANCE.convertRegisterPostDTOtoUser(dto);

        assertNull(user.getUserId(),       "userId must be ignored (set by JPA)");
        assertNull(user.getToken(),        "token must be ignored (set at login)");
        assertNull(user.getCreationDate(), "creationDate must be ignored (set by @PrePersist)");
        assertNull(user.getUserScoreboard(),"userScoreboard must be ignored (set in service)");
        assertNull(user.getIsOnline(),     "isOnline must be ignored (set in service)");
    }

    // ═══════════════════════════════════════════════════════════════════
    // convertUsertoUserAuthDTO
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: userId und token werden korrekt in das Auth-DTO uebertragen.
     * Faengt Bug: Vertauschte Felder wuerden den Client mit einem fremden Token
     * authentifizieren.
     */
    @Test
    void convertUsertoUserAuthDTO_mapsUserIdAndToken() {
        User user = buildUser(7L, "charlie", "charlie@uzh.ch", "pw");
        user.setToken("abc-token");

        UserAuthDTO dto = DTOMapper.INSTANCE.convertUsertoUserAuthDTO(user);

        assertEquals(7L,          dto.getUserId());
        assertEquals("abc-token", dto.getToken());
    }

    // ═══════════════════════════════════════════════════════════════════
    // convertUserToUserDTO
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: username und userBio werden aus UserProfile herausgemappt.
     * Faengt Bug: Wenn die @Mapping-Pfad-Angaben fehlen, wuerden username
     * und userBio null sein (MapStruct kann nicht automatisch durch
     * @Embedded navigieren ohne explizite source-Pfade).
     */
    @Test
    void convertUserToUserDTO_projectsUsernameAndBioFromProfile() {
        User user = buildUser(3L, "dana", "dana@uzh.ch", "pw");
        user.getUserProfile().setUserBio("bio text");

        UserDTO dto = DTOMapper.INSTANCE.convertUserToUserDTO(user);

        assertEquals(3L,         dto.getUserId());
        assertEquals("dana",     dto.getUsername());
        assertEquals("bio text", dto.getUserBio());
    }

    /**
     * Prueft: Das password-Feld aus UserProfile erscheint NICHT im UserDTO.
     * Faengt Bug: SECURITY-KRITISCH. Ein versehentliches @Mapping auf password
     * wuerde Passwort-Hashes an jeden Client leaken, der diesen Endpoint aufruft.
     */
    @Test
    void convertUserToUserDTO_doesNotExposePassword() {
        User user = buildUser(4L, "eve", "eve@uzh.ch", "should-not-appear");

        UserDTO dto = DTOMapper.INSTANCE.convertUserToUserDTO(user);

        // UserDTO has no password field by design — this test documents that
        // no accidental mapping adds one in the future.
        // We verify by confirming the DTO type itself has no password accessor:
        assertDoesNotThrow(() -> dto.getClass().getMethod("getUserId"));
        assertThrows(NoSuchMethodException.class,
                () -> dto.getClass().getMethod("getPassword"),
                "UserDTO must not expose a getPassword() method");
    }

    // ═══════════════════════════════════════════════════════════════════
    // convertUserToMyUserDTO
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: username, email, userBio und das Scoreboard werden korrekt
     * aus den eingebetteten Objekten ins MyUserDTO projiziert.
     * Faengt Bug: Die expliziten Scoreboard-Mapping-Pfade muessen alle
     * Unterfelder benennen — ein fehlender Pfad laesst das Feld null.
     */
    @Test
    void convertUserToMyUserDTO_mapsProfileAndScoreboard() {
        User user = buildUser(5L, "frank", "frank@uzh.ch", "pw");
        user.getUserProfile().setUserBio("traveller");
        user.setIsOnline(true);
        user.setIsGuest(false);

        UserScoreboard scoreboard = new UserScoreboard();
        scoreboard.setTotalPoints(100L);
        scoreboard.setPlayedGames(10L);
        scoreboard.setPlayedRounds(30L);
        scoreboard.setBestRoundPoints(25L);
        scoreboard.setGuessingPrecision(0.75f);
        user.setUserScoreboard(scoreboard);

        MyUserDTO dto = DTOMapper.INSTANCE.convertUserToMyUserDTO(user);

        assertEquals(5L,          dto.getUserId());
        assertEquals("frank",     dto.getUsername());
        assertEquals("frank@uzh.ch", dto.getEmail());
        assertEquals("traveller", dto.getUserBio());
        assertNotNull(dto.getUserScoreboard());
        assertEquals(100L,  dto.getUserScoreboard().getTotalPoints());
        assertEquals(10L,   dto.getUserScoreboard().getPlayedGames());
        assertEquals(30L,   dto.getUserScoreboard().getPlayedRounds());
        assertEquals(25L,   dto.getUserScoreboard().getBestRoundPoints());
        assertEquals(0.75f, dto.getUserScoreboard().getGuessingPrecision());
    }

    /**
     * Prueft: Das friends-Feld ist im MyUserDTO null (explizit ignoriert).
     * Faengt Bug: Wenn friends nicht ignoriert wuerde, wuerde MapStruct
     * versuchen, die players-Liste o.ae. zuzuordnen — falsche Daten im DTO.
     */
    @Test
    void convertUserToMyUserDTO_friendsIsNull() {
        User user = buildUser(6L, "grace", "grace@uzh.ch", "pw");
        user.setUserScoreboard(new UserScoreboard());

        MyUserDTO dto = DTOMapper.INSTANCE.convertUserToMyUserDTO(user);

        assertNull(dto.getFriends(), "friends must be ignored in the mapping");
    }

    // ═══════════════════════════════════════════════════════════════════
    // convertEntityToLobbyDTO
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: Alle direkt gemappten Lobby-Felder landen korrekt im LobbyDTO.
     * Faengt Bug: Ein versehentlich falsch benanntes target-Feld wuerde null
     * in der Lobby-Liste im Frontend produzieren.
     */
    @Test
    void convertEntityToLobbyDTO_mapsAllDirectFields() {
        Lobby lobby = buildLobby(1L, "TestLobby", "ABCD",
                LobbyVisibility.PUBLIC, LobbyState.WAITING, 4, 5);

        LobbyDTO dto = DTOMapper.INSTANCE.convertEntityToLobbyDTO(lobby);

        assertEquals(1L,                    dto.getLobbyId());
        assertEquals("TestLobby",           dto.getLobbyName());
        assertEquals("ABCD",                dto.getLobbyCode());
        assertEquals(LobbyVisibility.PUBLIC, dto.getVisibility());
        assertEquals(LobbyState.WAITING,    dto.getLobbyState());
        assertEquals(4,                     dto.getMaxPlayers());
        assertEquals(5,                     dto.getMaxRounds());
    }

    /**
     * Prueft: currentPlayers ist null (explizit ignoriert — wird vom Controller
     * manuell nach dem Mapping gesetzt).
     * Faengt Bug: Wenn currentPlayers nicht ignoriert wuerde, wuerde MapStruct
     * 0 oder null hineinschreiben statt den echten Wert aus players.size().
     */
    @Test
    void convertEntityToLobbyDTO_currentPlayersIsNull() {
        Lobby lobby = buildLobby(2L, "L", "BCDE",
                LobbyVisibility.PUBLIC, LobbyState.WAITING, 2, 3);

        LobbyDTO dto = DTOMapper.INSTANCE.convertEntityToLobbyDTO(lobby);

        assertNull(dto.getCurrentPlayers(),
                "currentPlayers must be ignored — set manually by the controller");
    }

    // ═══════════════════════════════════════════════════════════════════
    // convertEntityToMyLobbyDTO
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: adminId wird aus admin.userId extrahiert, und die players-Liste
     * wird korrekt uebertragen.
     * Faengt Bug: Wenn "admin.userId" als source fehlt, wuerde adminId null sein
     * und der Client wuerde nicht wissen, wer der Admin ist.
     */
    @Test
    void convertEntityToMyLobbyDTO_mapsAdminIdAndPlayers() {
        User admin = buildUser(10L, "admin", "admin@uzh.ch", "pw");
        User player = buildUser(11L, "player", "player@uzh.ch", "pw");

        Lobby lobby = buildLobby(3L, "MyLobby", "EFGH",
                LobbyVisibility.PRIVATE, LobbyState.IN_GAME, 4, 3);
        lobby.setAdmin(admin);
        lobby.setPlayers(new ArrayList<>(List.of(admin, player)));

        MyLobbyDTO dto = DTOMapper.INSTANCE.convertEntityToMyLobbyDTO(lobby);

        assertEquals(3L,                     dto.getLobbyId());
        assertEquals("MyLobby",              dto.getLobbyName());
        assertEquals("EFGH",                 dto.getLobbyCode());
        assertEquals(10L,                    dto.getAdminId());
        assertEquals(LobbyVisibility.PRIVATE, dto.getVisibility());
        assertEquals(LobbyState.IN_GAME,     dto.getLobbyState());
        assertNotNull(dto.getPlayers());
        assertEquals(2, dto.getPlayers().size());
    }

    /**
     * Prueft: currentPlayers ist null in MyLobbyDTO (ebenfalls explizit ignoriert).
     */
    @Test
    void convertEntityToMyLobbyDTO_currentPlayersIsNull() {
        User admin = buildUser(12L, "host", "host@uzh.ch", "pw");
        Lobby lobby = buildLobby(4L, "L2", "FGHI",
                LobbyVisibility.PUBLIC, LobbyState.WAITING, 4, 3);
        lobby.setAdmin(admin);

        MyLobbyDTO dto = DTOMapper.INSTANCE.convertEntityToMyLobbyDTO(lobby);

        assertNull(dto.getCurrentPlayers(),
                "currentPlayers must be ignored — set manually by the controller");
    }

    // ═══════════════════════════════════════════════════════════════════
    // convertUserAchievementToUserAchievementDTO
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: userId wird aus userAchievement.user.userId extrahiert.
     * Faengt Bug: Wenn der Pfad "user.userId" fehlt, wuerde userId null sein
     * und das Frontend koennte Achievement-Eintraege nicht dem richtigen User
     * zuordnen.
     */
    @Test
    void convertUserAchievementToUserAchievementDTO_mapsUserIdFromNestedUser() {
        User user = buildUser(20L, "hank", "hank@uzh.ch", "pw");
        Achievement achievement = new Achievement();

        UserAchievement ua = new UserAchievement();
        ua.setUser(user);
        ua.setAchievement(achievement);

        UserAchievementDTO dto =
                DTOMapper.INSTANCE.convertUserAchievementToUserAchievementDTO(ua);

        assertEquals(20L, dto.getUserId());
        assertEquals(achievement, dto.getAchievement());
    }

    // ═══════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════

    private User buildUser(Long id, String username, String email, String password) {
        User user = new User();
        user.setUserId(id);
        UserProfile profile = new UserProfile();
        profile.setUsername(username);
        profile.setEmail(email);
        profile.setPassword(password);
        user.setUserProfile(profile);
        user.setIsOnline(false);
        user.setIsGuest(false);
        return user;
    }

    private Lobby buildLobby(Long id, String name, String code,
                              LobbyVisibility visibility, LobbyState state,
                              int maxPlayers, int maxRounds) {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(id);
        lobby.setLobbyName(name);
        lobby.setLobbyCode(code);
        lobby.setVisibility(visibility);
        lobby.setLobbyState(state);
        lobby.setMaxPlayers(maxPlayers);
        lobby.setMaxRounds(maxRounds);
        lobby.setCurrentRound(0);
        lobby.setPlayers(new ArrayList<>());
        return lobby;
    }
}
