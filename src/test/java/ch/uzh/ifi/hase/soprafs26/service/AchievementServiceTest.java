
package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.repository.*;
import ch.uzh.ifi.hase.soprafs26.websocket.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AchievementServiceTest {

    @Mock private AchievementRepository achievementRepository;
    @Mock private UserAchievementRepository userAchievementRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private GuessRepository guessRepository;
    @Mock private SimpMessagingTemplate simpMessagingTemplate;

    @InjectMocks
    private AchievementService achievementService;

    // ── Shared test fixtures ──────────────────────────────────────────────────

    private User user;
    private UserProfile userProfile;
    private UserScoreboard scoreboard;
    private Lobby lobby;

    /** Each call produces a unique achievementId so the earnedAchievementIds
     *  Set inside awardIfMissing never accidentally deduplicates two distinct awards. */
    private final AtomicLong achievementIdSeq = new AtomicLong(1L);

    @BeforeEach
    void setUp() {
        userProfile = new UserProfile();
        userProfile.setUsername("testUser");

        scoreboard = new UserScoreboard();
        scoreboard.setPlayedGames(0L);
        scoreboard.setPlayedRounds(0L);
        scoreboard.setTotalPoints(0L);
        scoreboard.setBestRoundPoints(0L);

        user = new User();
        user.setUserId(1L);
        user.setIsGuest(false);
        user.setUserProfile(userProfile);
        user.setUserScoreboard(scoreboard);

        lobby = new Lobby();
        lobby.setPlayers(List.of(user));
    }

    // ── Helper: stub a named achievement that the user does not yet own ───────
    // Returns the Achievement so callers can reuse it if needed.

    private Achievement stubAchievementNotOwned(String name) {
        Achievement ach = new Achievement();
        ach.setAchievementId(achievementIdSeq.getAndIncrement()); // unique id per call
        ach.setName(name);
        when(achievementRepository.findByName(name)).thenReturn(ach);
        when(userAchievementRepository.findAll()).thenReturn(Collections.emptyList());
        return ach;
    }

    // ── evaluateAchievementsForLobby ─────────────────────────────────────────

    @Test
    void evaluateAchievementsForLobby_skipsGuestUsers() {
        user.setIsGuest(true);
        when(roundRepository.findByLobbyOrderByRoundNumberAsc(lobby)).thenReturn(Collections.emptyList());

        achievementService.evaluateAchievementsForLobby(lobby);

        verifyNoInteractions(achievementRepository);
        verifyNoInteractions(userAchievementRepository);
    }

    @Test
    void evaluateAchievementsForLobby_callsEvaluateForEachNonGuestPlayer() {
        User guest = new User();
        guest.setUserId(2L);
        guest.setIsGuest(true);

        User nonGuest = new User();
        nonGuest.setUserId(3L);
        nonGuest.setIsGuest(false);
        UserProfile profile = new UserProfile();
        profile.setUsername("nonGuest");
        nonGuest.setUserProfile(profile);
        nonGuest.setUserScoreboard(null); // null scoreboard → early return

        lobby.setPlayers(List.of(guest, nonGuest));
        when(roundRepository.findByLobbyOrderByRoundNumberAsc(lobby)).thenReturn(Collections.emptyList());

        achievementService.evaluateAchievementsForLobby(lobby);

        // guest skipped; non-guest exits early due to null scoreboard → no repo calls
        verifyNoInteractions(achievementRepository);
    }

    // ── evaluateAchievementsForUser – null scoreboard guard ──────────────────

    @Test
    void evaluateAchievementsForUser_doesNothingWhenScoreboardIsNull() {
        user.setUserScoreboard(null);

        achievementService.evaluateAchievementsForUser(user, lobby, Collections.emptyList());

        verifyNoInteractions(achievementRepository);
    }

    // ── Rookie Traveler ───────────────────────────────────────────────────────

    @Test
    void evaluateAchievementsForUser_awardsRookieTravelerWhenPlayedGamesIsOne() {
        scoreboard.setPlayedGames(1L);
        stubAchievementNotOwned("Rookie Traveler");

        achievementService.evaluateAchievementsForUser(user, lobby, Collections.emptyList());

        verify(userAchievementRepository).save(any(UserAchievement.class));
        verify(simpMessagingTemplate).convertAndSend(
                eq("/topic/1/notifications"), any(Message.class));
    }

    @Test
    void evaluateAchievementsForUser_doesNotAwardRookieTravelerWhenZeroGames() {
        // playedGames == 0 → the condition is never true → awardIfMissing is never
        // called for Rookie Traveler → no stub needed, just verify no save happens.
        achievementService.evaluateAchievementsForUser(user, lobby, Collections.emptyList());

        verify(userAchievementRepository, never()).save(any());
    }

    // ── Seasoned Traveler ─────────────────────────────────────────────────────

    @Test
    void evaluateAchievementsForUser_awardsSeasonedTravelerAtTenGames() {
        scoreboard.setPlayedGames(10L);

        // Give each achievement a distinct id so earnedAchievementIds doesn't
        // block the second award when both are evaluated in the same call.
        stubAchievementNotOwned("Rookie Traveler");
        stubAchievementNotOwned("Seasoned Traveler");

        achievementService.evaluateAchievementsForUser(user, lobby, Collections.emptyList());

        verify(userAchievementRepository, times(2)).save(any(UserAchievement.class));
    }

    @Test
    void evaluateAchievementsForUser_doesNotAwardSeasonedTravelerBelowTen() {
        scoreboard.setPlayedGames(9L);
        // Only Rookie Traveler condition is met; stub only that one.
        stubAchievementNotOwned("Rookie Traveler");

        achievementService.evaluateAchievementsForUser(user, lobby, Collections.emptyList());

        verify(userAchievementRepository, times(1)).save(any(UserAchievement.class));
    }

    // ── Swiss Rail Expert ─────────────────────────────────────────────────────

    @Test
    void evaluateAchievementsForUser_awardsSwissRailExpertAtTenThousandPoints() {
        scoreboard.setTotalPoints(10000L);
        stubAchievementNotOwned("Swiss Rail Expert");

        achievementService.evaluateAchievementsForUser(user, lobby, Collections.emptyList());

        verify(userAchievementRepository).save(any(UserAchievement.class));
    }

    @Test
    void evaluateAchievementsForUser_doesNotAwardSwissRailExpertBelow10000() {
        // totalPoints < 10000 → condition never true → no stub needed
        scoreboard.setTotalPoints(9999L);

        achievementService.evaluateAchievementsForUser(user, lobby, Collections.emptyList());

        verify(userAchievementRepository, never()).save(any());
    }

    // ── Frequent Flyer ────────────────────────────────────────────────────────

    @Test
    void evaluateAchievementsForUser_awardsFrequentFlyerAt50Rounds() {
        scoreboard.setPlayedRounds(50L);
        stubAchievementNotOwned("Frequent Flyer");

        achievementService.evaluateAchievementsForUser(user, lobby, Collections.emptyList());

        verify(userAchievementRepository).save(any(UserAchievement.class));
    }

    @Test
    void evaluateAchievementsForUser_doesNotAwardFrequentFlyerBelow50() {
        // playedRounds < 50 → condition never true → no stub needed
        scoreboard.setPlayedRounds(49L);

        achievementService.evaluateAchievementsForUser(user, lobby, Collections.emptyList());

        verify(userAchievementRepository, never()).save(any());
    }

    // ── Conductor (multiplayer win) ───────────────────────────────────────────

    @Test
    void evaluateAchievementsForUser_awardsConductorWhenUserWinsMultiplayerGame() {
        scoreboard.setTotalPoints(500L);
        user.setUserScoreboard(scoreboard);

        // Setup another user
        User otherUser = new User();
        otherUser.setUserId(2L);

        // Configure the lobby to explicitly set the winner
        lobby.setPlayers(List.of(user, otherUser));
        lobby.setWinner(user); // This is now required by isMultiplayerWin(user, lobby)

        stubAchievementNotOwned("Conductor");

        // Execute
        achievementService.evaluateAchievementsForUser(user, lobby, Collections.emptyList());

        // Verify
        verify(userAchievementRepository).save(any(UserAchievement.class));
    }
    @Test
    void evaluateAchievementsForUser_doesNotAwardConductorInSinglePlayerLobby() {
        // Even with high points, it's not a multiplayer win
        scoreboard.setTotalPoints(999L);
        lobby.setPlayers(List.of(user));
        // Winner is null or someone else; specifically not the user
        lobby.setWinner(null);

        achievementService.evaluateAchievementsForUser(user, lobby, Collections.emptyList());

        verify(userAchievementRepository, never()).save(any());
    }

    @Test
    void evaluateAchievementsForUser_doesNotAwardConductorWhenUserLoses() {
        User winner = new User();
        winner.setUserId(2L);

        lobby.setPlayers(List.of(user, winner));
        // Explicitly set another user as the winner
        lobby.setWinner(winner);

        achievementService.evaluateAchievementsForUser(user, lobby, Collections.emptyList());

        verify(userAchievementRepository, never()).save(any());
    }
    // ── Close Call ────────────────────────────────────────────────────────────

    @Test
    void evaluateAchievementsForUser_awardsCloseCallWhenDistanceLe0_5km() {
        Round round = new Round();
        Guess guess = new Guess();
        guess.setDistanceToTrain(0.4f);
        guess.setPoints(500);

        when(guessRepository.findByRoundAndUserUserId(round, 1L)).thenReturn(guess);
        stubAchievementNotOwned("Close Call");

        achievementService.evaluateAchievementsForUser(user, lobby, List.of(round));

        verify(userAchievementRepository).save(any(UserAchievement.class));
    }

    @Test
    void evaluateAchievementsForUser_doesNotAwardCloseCallWhenDistanceAbove0_5km() {
        // distance > 0.5 → hasCloseCall stays false → awardIfMissing never called → no stub needed
        Round round = new Round();
        Guess guess = new Guess();
        guess.setDistanceToTrain(1.0f);
        guess.setPoints(500);

        when(guessRepository.findByRoundAndUserUserId(round, 1L)).thenReturn(guess);

        achievementService.evaluateAchievementsForUser(user, lobby, List.of(round));

        verify(userAchievementRepository, never()).save(any());
    }

    // ── Perfect Round ─────────────────────────────────────────────────────────

    @Test
    void evaluateAchievementsForUser_awardsPerfectRoundWhenPointsAre1000() {
        Round round = new Round();
        Guess guess = new Guess();
        guess.setDistanceToTrain(0.1f); // also triggers Close Call
        guess.setPoints(1000);

        when(guessRepository.findByRoundAndUserUserId(round, 1L)).thenReturn(guess);
        // Both flags fire; give each a distinct id
        stubAchievementNotOwned("Close Call");
        stubAchievementNotOwned("Perfect Round");

        achievementService.evaluateAchievementsForUser(user, lobby, List.of(round));

        verify(userAchievementRepository, times(2)).save(any(UserAchievement.class));
    }

    // ── Emergency Stop ────────────────────────────────────────────────────────

    @Test
    void evaluateAchievementsForUser_awardsEmergencyStopWhenPointsAreZero() {
        Round round = new Round();
        Guess guess = new Guess();
        guess.setDistanceToTrain(200.0f); // also triggers Wrong Train
        guess.setPoints(0);

        when(guessRepository.findByRoundAndUserUserId(round, 1L)).thenReturn(guess);
        stubAchievementNotOwned("Emergency Stop");
        stubAchievementNotOwned("Wrong Train!");

        achievementService.evaluateAchievementsForUser(user, lobby, List.of(round));

        verify(userAchievementRepository, times(2)).save(any(UserAchievement.class));
    }

    // ── Wrong Train ───────────────────────────────────────────────────────────

    @Test
    void evaluateAchievementsForUser_awardsWrongTrainWhenDistanceOver100km() {
        Round round = new Round();
        Guess guess = new Guess();
        guess.setDistanceToTrain(150.0f);
        guess.setPoints(0); // also triggers Emergency Stop

        when(guessRepository.findByRoundAndUserUserId(round, 1L)).thenReturn(guess);
        stubAchievementNotOwned("Wrong Train!");
        stubAchievementNotOwned("Emergency Stop");

        achievementService.evaluateAchievementsForUser(user, lobby, List.of(round));

        verify(userAchievementRepository, times(2)).save(any(UserAchievement.class));
    }

    @Test
    void evaluateAchievementsForUser_doesNotAwardWrongTrainWhenDistanceBelow100km() {
        // distance <= 100 → hasWrongTrain stays false → awardIfMissing never called → no stub needed
        Round round = new Round();
        Guess guess = new Guess();
        guess.setDistanceToTrain(99.9f);
        guess.setPoints(500);

        when(guessRepository.findByRoundAndUserUserId(round, 1L)).thenReturn(guess);

        achievementService.evaluateAchievementsForUser(user, lobby, List.of(round));

        verify(userAchievementRepository, never()).save(any());
    }

    // ── Train Transfer (5 consecutive rounds > 800 pts) ───────────────────────

    @Test
    void evaluateAchievementsForUser_awardsTrainTransferAfterFiveConsecutiveOver800() {
        List<Round> rounds = List.of(
                new Round(), new Round(), new Round(), new Round(), new Round()
        );

        for (Round round : rounds) {
            Guess guess = new Guess();
            guess.setPoints(850);
            guess.setDistanceToTrain(1.0f);
            when(guessRepository.findByRoundAndUserUserId(round, 1L)).thenReturn(guess);
        }

        stubAchievementNotOwned("Train Transfer");

        achievementService.evaluateAchievementsForUser(user, lobby, rounds);

        verify(userAchievementRepository).save(any(UserAchievement.class));
    }

    @Test
    void evaluateAchievementsForUser_doesNotAwardTrainTransferWhenStreakBroken() {
        // Streak resets at r3 → never reaches 5 consecutive → awardIfMissing never called → no stub needed
        Round r1 = new Round();
        Round r2 = new Round();
        Round r3 = new Round();
        Round r4 = new Round();
        Round r5 = new Round();

        Guess highGuess = new Guess();
        highGuess.setPoints(850);
        highGuess.setDistanceToTrain(1.0f);

        Guess lowGuess = new Guess();
        lowGuess.setPoints(400);
        lowGuess.setDistanceToTrain(5.0f);

        when(guessRepository.findByRoundAndUserUserId(r1, 1L)).thenReturn(highGuess);
        when(guessRepository.findByRoundAndUserUserId(r2, 1L)).thenReturn(highGuess);
        when(guessRepository.findByRoundAndUserUserId(r3, 1L)).thenReturn(lowGuess); // resets counter
        when(guessRepository.findByRoundAndUserUserId(r4, 1L)).thenReturn(highGuess);
        when(guessRepository.findByRoundAndUserUserId(r5, 1L)).thenReturn(highGuess);

        achievementService.evaluateAchievementsForUser(user, lobby, List.of(r1, r2, r3, r4, r5));

        verify(userAchievementRepository, never()).save(any());
    }

    // ── awardIfMissing – already owned guard ──────────────────────────────────

    @Test
    void evaluateAchievementsForUser_doesNotAwardAlreadyOwnedAchievement() {
        scoreboard.setPlayedGames(1L);

        Achievement rookieAch = new Achievement();
        rookieAch.setAchievementId(10L);
        rookieAch.setName("Rookie Traveler");
        when(achievementRepository.findByName("Rookie Traveler")).thenReturn(rookieAch);

        UserAchievement existing = new UserAchievement();
        existing.setUser(user);
        existing.setAchievement(rookieAch);
        when(userAchievementRepository.findAll()).thenReturn(List.of(existing));

        achievementService.evaluateAchievementsForUser(user, lobby, Collections.emptyList());

        verify(userAchievementRepository, never()).save(any());
        verify(simpMessagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    // ── awardIfMissing – null achievement from repo ───────────────────────────

    @Test
    void evaluateAchievementsForUser_handlesNullAchievementFromRepository() {
        scoreboard.setPlayedGames(1L);
        when(achievementRepository.findByName(any())).thenReturn(null);

        achievementService.evaluateAchievementsForUser(user, lobby, Collections.emptyList());

        verify(userAchievementRepository, never()).save(any());
    }

    // ── WebSocket notification ────────────────────────────────────────────────

    @Test
    void evaluateAchievementsForUser_sendsWebSocketNotificationOnAward() {
        scoreboard.setPlayedGames(1L);
        stubAchievementNotOwned("Rookie Traveler");

        achievementService.evaluateAchievementsForUser(user, lobby, Collections.emptyList());

        verify(simpMessagingTemplate).convertAndSend(
                eq("/topic/1/notifications"),
                any(Message.class));
    }

    // ── Null guess guard ──────────────────────────────────────────────────────

    @Test
    void evaluateAchievementsForUser_skipsRoundWhenGuessIsNull() {
        // Null guess → round skipped → no round-based achievement conditions fire
        // → awardIfMissing never called → no stub needed
        Round round = new Round();
        when(guessRepository.findByRoundAndUserUserId(round, 1L)).thenReturn(null);

        achievementService.evaluateAchievementsForUser(user, lobby, List.of(round));

        verify(userAchievementRepository, never()).save(any());
    }
}
