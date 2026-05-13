package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Achievement;
import ch.uzh.ifi.hase.soprafs26.entity.Guess;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.Round;
import ch.uzh.ifi.hase.soprafs26.entity.RoundHistory;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.UserAchievement;
import ch.uzh.ifi.hase.soprafs26.entity.UserScoreboard;
import ch.uzh.ifi.hase.soprafs26.repository.AchievementRepository;
import ch.uzh.ifi.hase.soprafs26.repository.GuessRepository;
import ch.uzh.ifi.hase.soprafs26.repository.RoundHistoryRepository;
import ch.uzh.ifi.hase.soprafs26.repository.RoundRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserAchievementRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final RoundHistoryRepository roundHistoryRepository;
    private final RoundRepository roundRepository;
    private final GuessRepository guessRepository;
    private final UserRepository userRepository;

    public AchievementService(AchievementRepository achievementRepository,
                              UserAchievementRepository userAchievementRepository,
                              RoundHistoryRepository roundHistoryRepository,
                              RoundRepository roundRepository,
                              GuessRepository guessRepository,
                              UserRepository userRepository) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.roundHistoryRepository = roundHistoryRepository;
        this.roundRepository = roundRepository;
        this.guessRepository = guessRepository;
        this.userRepository = userRepository;
    }

    public void evaluateAchievementsForLobby(Lobby lobby) {
        List<Round> rounds = roundRepository.findByLobbyOrderByRoundNumberAsc(lobby);

        for (User user : lobby.getPlayers()) {
            if (user.getIsGuest() == false) {
                System.out.println("[AchievementService] achievement evaluation for user '" + user.getUserProfile().getUsername() + "'");
                evaluateAchievementsForUser(user, lobby, rounds);
            }
        }
    }

    public void evaluateAchievementsForUser(User user, Lobby lobby, List<Round> rounds) {
        UserScoreboard scoreboard = user.getUserScoreboard();
        if (scoreboard == null) {
            return;
        }

        long playedGames = scoreboard.getPlayedGames() != null ? scoreboard.getPlayedGames() : 0L;
        long playedRounds = scoreboard.getPlayedRounds() != null ? scoreboard.getPlayedRounds() : 0L;
        long totalPoints = scoreboard.getTotalPoints() != null ? scoreboard.getTotalPoints() : 0L;
        long bestRoundPoints = scoreboard.getBestRoundPoints() != null ? scoreboard.getBestRoundPoints() : 0L;

        // Load current round history for this user

        // Load guesses from this lobby to inspect distance / points
        Set<Long> earnedAchievementIds = new HashSet<>();

        // 1) Rookie Traveler
        if (playedGames >= 1) {
            awardIfMissing(user, "Rookie Traveler", earnedAchievementIds);
        }

        // 2) Seasoned Traveler
        if (playedGames >= 10) {
            awardIfMissing(user, "Seasoned Traveler", earnedAchievementIds);
        }

        // 3) Swiss Rail Expert
        if (totalPoints >= 10000) {
            awardIfMissing(user, "Swiss Rail Expert", earnedAchievementIds);
        }

        // 4) Frequent Flyer
        if (playedRounds >= 50) {
            awardIfMissing(user, "Frequent Flyer", earnedAchievementIds);
        }

        // 5) Conductor
        if (isMultiplayerWin(user, lobby)) {
            awardIfMissing(user, "Conductor", earnedAchievementIds);
        }

        // Round-based checks
        boolean hasCloseCall = false;
        boolean hasPerfectRound = false;
        boolean hasEmergencyStop = false;
        boolean hasWrongTrain = false;
        int consecutiveOver800 = 0;
        boolean trainTransfer = false;

        for (Round round : rounds) {
            Guess guess = guessRepository.findByRoundAndUserUserId(round, user.getUserId());
            if (guess == null) {
                continue;
            }

            int points = guess.getPoints() != null ? guess.getPoints() : 0;
            float distanceKm = guess.getDistanceToTrain() != null ? guess.getDistanceToTrain() : Float.MAX_VALUE;

            if (distanceKm <= 0.5f) {
                hasCloseCall = true;
            }
            if (points == 1000) {
                hasPerfectRound = true;
            }
            if (points == 0) {
                hasEmergencyStop = true;
            }
            if (distanceKm > 100.0f) {
                hasWrongTrain = true;
            }

            if (points > 800) {
                consecutiveOver800++;
                if (consecutiveOver800 >= 5) {
                    trainTransfer = true;
                }
            } else {
                consecutiveOver800 = 0;
            }
        }

        if (hasCloseCall) {
            awardIfMissing(user, "Close Call", earnedAchievementIds);
        }
        if (hasPerfectRound) {
            awardIfMissing(user, "Perfect Round", earnedAchievementIds);
        }
        if (trainTransfer) {
            awardIfMissing(user, "Train Transfer", earnedAchievementIds);
        }
        if (hasEmergencyStop) {
            awardIfMissing(user, "Emergency Stop", earnedAchievementIds);
        }
        if (hasWrongTrain) {
            awardIfMissing(user, "Wrong Train!", earnedAchievementIds);
        }
    }

    private boolean isMultiplayerWin(User user, Lobby lobby) {
        // Adapt this to your actual lobby/player-winner logic if you already have one.
        // This fallback assumes "winning" means highest totalPoints in the lobby.
        long userPoints = user.getUserScoreboard() != null && user.getUserScoreboard().getTotalPoints() != null
                ? user.getUserScoreboard().getTotalPoints()
                : 0L;

        long maxPoints = 0L;
        for (User player : lobby.getPlayers()) {
            UserScoreboard scoreboard = player.getUserScoreboard();
            long points = scoreboard != null && scoreboard.getTotalPoints() != null
                    ? scoreboard.getTotalPoints()
                    : 0L;
            maxPoints = Math.max(maxPoints, points);
        }

        boolean isWinner = userPoints == maxPoints;
        return lobby.getPlayers() != null && lobby.getPlayers().size() > 1 && isWinner;
    }

    private void awardIfMissing(User user, String achievementName, Set<Long> earnedAchievementIds) {
        Achievement achievement = achievementRepository.findByName(achievementName);

        if (achievement == null) {
            return;
        }

        if (earnedAchievementIds.contains(achievement.getAchievementId())) {
            return;
        }

        boolean alreadyOwned = userAchievementRepository.findAll()
                .stream()
                .anyMatch(ua ->
                        ua.getUser().getUserId().equals(user.getUserId())
                                && ua.getAchievement().getAchievementId().equals(achievement.getAchievementId()));

        if (alreadyOwned) {
            return;
        }

        UserAchievement userAchievement = new UserAchievement();
        userAchievement.setUser(user);
        userAchievement.setAchievement(achievement);
        userAchievementRepository.save(userAchievement);
        System.out.println("[AchievementService]Awarded achievement '" + achievementName + "' to user '" + user.getUserProfile().getUsername() + "'");
        earnedAchievementIds.add(achievement.getAchievementId());
    }
}