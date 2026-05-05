package ch.uzh.ifi.hase.soprafs26.service;


import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;
import ch.uzh.ifi.hase.soprafs26.constant.MessageType;
import ch.guessbb.sopraserver.entity.*;
import ch.guessbb.sopraserver.objects.*;
import ch.guessbb.sopraserver.repository.*;
import ch.uzh.ifi.hase.soprafs26.entity.*;
import ch.uzh.ifi.hase.soprafs26.objects.Train;
import ch.uzh.ifi.hase.soprafs26.objects.UserGameStatus;
import ch.uzh.ifi.hase.soprafs26.objects.UserResult;
import ch.uzh.ifi.hase.soprafs26.repository.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessMessageDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ResultDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RoundStartDTO;
import ch.uzh.ifi.hase.soprafs26.trains.TrainPositionFetcher;
import ch.uzh.ifi.hase.soprafs26.websocket.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;


@Service
@Transactional
public class GameService {
    private final TrainPositionFetcher trainPositionFetcher;
    private final RoundRepository roundRepository;
    private final GuessRepository guessRepository;
    private final LobbyRepository lobbyRepository;
    private final UserRepository userRepository;
    private final Map<Long, ScheduledFuture<?>> activeTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<Long, Boolean> scoresPublished = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final RoundHistoryRepository roundHistoryRepository;

    public GameService(TrainPositionFetcher trainPositionFetcher, RoundRepository roundRepository, GuessRepository guessRepository, LobbyRepository lobbyRepository, UserRepository userRepository, SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper, RoundHistoryRepository roundHistoryRepository) {
        this.trainPositionFetcher = trainPositionFetcher;
        this.roundRepository = roundRepository;
        this.guessRepository = guessRepository;
        this.lobbyRepository = lobbyRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.roundHistoryRepository = roundHistoryRepository;
    }

    public void setupGame(Lobby currentLobby) {
        try {
            List<Train> trains = trainPositionFetcher.fetchTrains(currentLobby.getMaxRounds());
            for (Train train : trains) {
                trainPositionFetcher.interpolatePosition(train);
            }

            Long lobbyId = currentLobby.getLobbyId();
            scoresPublished.put(lobbyId, false);

            for (int i = 0; i < currentLobby.getMaxRounds(); i++) {
                Round round = new Round();
                round.setLobby(currentLobby);
                round.setRoundNumber(i + 1);
                round.setTrainData(objectMapper.writeValueAsString(trains.get(i)));
                roundRepository.save(round);

                for (User player : currentLobby.getPlayers()) {
                    Guess guess = new Guess();
                    guess.setRound(round);
                    guess.setUser(player);
                    guess.setHasGuessed(false);
                    guessRepository.save(guess);
                }
            }

            roundRepository.flush();
            guessRepository.flush();

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch trains", e);
        }
    }

    public void processGuessMessage(GuessMessageDTO guessMessage, Lobby currentLobby) {
        Long lobbyId = currentLobby.getLobbyId();
        Long userId = guessMessage.getUserId();

        if (!canSubmitGuess(lobbyId)) {
            return;
        }

        List<Round> rounds = roundRepository.findByLobbyOrderByRoundNumberAsc(currentLobby);
        int currentRoundNumber = currentLobby.getCurrentRound();
        Round currentRound = rounds.get(currentRoundNumber - 1);

        Train currentTrain;
        try {
            currentTrain = objectMapper.readValue(currentRound.getTrainData(), Train.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize train data", e);
        }

        double guessDistance = calculateGuessDistance(currentTrain, guessMessage.getXCoordinate(), guessMessage.getYCoordinate());
        int points = calculateScore(currentTrain, guessDistance);
        double roundedDistanceKm = Math.round((guessDistance / 1000.0) * 100.0) / 100.0;

        Guess guess = guessRepository.findByRoundAndUserUserId(currentRound, userId);
        guess.setLat(guessMessage.getXCoordinate().floatValue());
        guess.setLon(guessMessage.getYCoordinate().floatValue());
        guess.setPoints(points);
        guess.setDistanceToTrain((float) roundedDistanceKm);
        guess.setHasGuessed(true);
        guessRepository.save(guess);

        boolean allGuessed = checkAllGuessed(currentRound);
        if (allGuessed) {
            ScheduledFuture<?> timer = activeTimers.get(lobbyId);
            if (timer != null) timer.cancel(false);
            allowedToPublish(lobbyId);
        }

        Message message = new Message(MessageType.GAME_STATE, userId);
        messagingTemplate.convertAndSend("/topic/game/" + lobbyId, message);
    }

    private boolean checkAllGuessed(Round round) {
        return guessRepository.findByRound(round)
                .stream()
                .allMatch(Guess::getHasGuessed);
    }

    public void readyForNextRound(UserGameStatus userGameStatus, Lobby currentLobby) {
        System.out.println("updateUserGameStatus called for user " + userGameStatus.getUserId());
        Boolean allAreReady = updateUserGameStatus(userGameStatus, currentLobby);
        System.out.println("allAreReady: " + allAreReady);
        if (allAreReady) {
            System.out.println("All ready! Starting round...");
            roundStart(currentLobby);
        }
    }

    private final Map<Long, Map<Long, Boolean>> roundReadyStatus = new ConcurrentHashMap<>();

    public Boolean updateUserGameStatus(UserGameStatus userGameStatus, Lobby currentLobby) {
        Long lobbyId = currentLobby.getLobbyId();
        Long userId = userGameStatus.getUserId();

        roundReadyStatus.computeIfAbsent(lobbyId, k -> new ConcurrentHashMap<>())
                .put(userId, userGameStatus.getIsReady());

        Map<Long, Boolean> readyMap = roundReadyStatus.get(lobbyId);

        for (User player : currentLobby.getPlayers()) {
            Boolean isReady = readyMap.get(player.getUserId());
            if (isReady == null || !isReady) {
                return false;
            }
        }

        roundReadyStatus.remove(lobbyId);
        return true;
    }

    public boolean canSubmitGuess(long gameId) {
        return activeTimers.containsKey(gameId);
    }

    public void roundStart(Lobby currentLobby) {
        Long lobbyId = currentLobby.getLobbyId();
        System.out.println("[roundStart] Called for lobby " + lobbyId);

        List<Round> rounds = roundRepository.findByLobbyOrderByRoundNumberAsc(currentLobby);
        System.out.println("[roundStart] Found " + rounds.size() + " rounds");

        int currentRoundNumber = currentLobby.getCurrentRound() + 1;
        currentLobby.setCurrentRound(currentRoundNumber);
        lobbyRepository.save(currentLobby);
        System.out.println("[roundStart] Current round number: " + currentRoundNumber);

        scoresPublished.put(lobbyId, false);

        Round currentRound = rounds.get(currentRoundNumber - 1);

        Train trainWithoutCoordinates;
        try {
            trainWithoutCoordinates = objectMapper.readValue(currentRound.getTrainData(), Train.class);
            trainWithoutCoordinates.setCurrentX(0);
            trainWithoutCoordinates.setCurrentY(0);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize train data", e);
        }

        RoundStartDTO roundStartDTO = new RoundStartDTO(currentRoundNumber, currentLobby.getMaxRounds(), trainWithoutCoordinates);
        Message message = new Message(MessageType.ROUND_START, roundStartDTO);
        System.out.println("[roundStart] Sending ROUND_START to /topic/game/" + lobbyId);
        messagingTemplate.convertAndSend("/topic/game/" + lobbyId, message);
        System.out.println("[roundStart] ROUND_START sent!");

        ScheduledFuture<?> timer = scheduler.schedule(
                () -> roundEnd(lobbyId),
                45,
                TimeUnit.SECONDS
        );
        activeTimers.put(lobbyId, timer);
        System.out.println("[roundStart] Timer scheduled for 45 seconds");
    }

    public void roundEnd(Long lobbyId) {
        System.out.println("[roundEnd] Called for lobby " + lobbyId);
        messagingTemplate.convertAndSend("/topic/game/" + lobbyId,
                new Message(MessageType.ROUND_END, null));

        ScheduledFuture<?> lastMessagesTimer = scheduler.schedule(
                () -> allowedToPublish(lobbyId),
                3,
                TimeUnit.SECONDS
        );
        activeTimers.put(lobbyId, lastMessagesTimer);
    }

    public void allowedToPublish(Long lobbyId) {
        if (!scoresPublished.get(lobbyId)) {
            Lobby freshLobby = lobbyRepository.findById(lobbyId)
                    .orElseThrow(() -> new RuntimeException("Lobby not found"));
            publishScores(freshLobby);
        }
    }

    public void publishScores(Lobby currentLobby) {
        Lobby freshLobby = lobbyRepository.findById(currentLobby.getLobbyId())
                .orElseThrow(() -> new RuntimeException("Lobby not found"));

        Long lobbyId = freshLobby.getLobbyId();
        System.out.println("[publishScores] Called for lobby " + lobbyId);

        activeTimers.remove(lobbyId);
        scoresPublished.put(lobbyId, true);

        List<Round> rounds = roundRepository.findByLobbyOrderByRoundNumberAsc(freshLobby);
        System.out.println("[publishScores] Found " + rounds.size() + " rounds");

        int currentRoundNumber = freshLobby.getCurrentRound();
        System.out.println("[publishScores] currentRoundNumber: " + currentRoundNumber);

        if (currentRoundNumber == 0) {
            System.out.println("[publishScores] ERROR: currentRoundNumber is 0!");
            return;
        }

        Round currentRound = rounds.get(currentRoundNumber - 1);

        Train train;
        try {
            train = objectMapper.readValue(currentRound.getTrainData(), Train.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize train data", e);
        }

        List<Guess> guesses = guessRepository.findByRound(currentRound);
        System.out.println("[publishScores] Found " + guesses.size() + " guesses");

        List<UserResult> userResults = new ArrayList<>();
        for (Guess guess : guesses) {
            Long userId = guess.getUser().getUserId();
            int totalPoints = roundRepository.findByLobbyOrderByRoundNumberAsc(freshLobby)
                    .stream()
                    .flatMap(r -> guessRepository.findByRound(r).stream())
                    .filter(g -> g.getUser().getUserId().equals(userId))
                    .mapToInt(g -> g.getPoints() != null ? g.getPoints() : 0)
                    .sum();
            int roundPoints = guess.getPoints() != null ? guess.getPoints() : 0;
            long xCoordinate = guess.getLat() != null ? guess.getLat().longValue() : 0;
            long yCoordinate = guess.getLon() != null ? guess.getLon().longValue() : 0;
            double distance = guess.getDistanceToTrain() != null ? guess.getDistanceToTrain() : Double.MAX_VALUE;
            userResults.add(new UserResult(userId, totalPoints, roundPoints, xCoordinate, yCoordinate, distance));
            System.out.println("[publishScores] UserResult: userId=" + userId + " roundPoints=" + roundPoints + " totalPoints=" + totalPoints);
        }

        ResultDTO resultDTO = new ResultDTO(currentRoundNumber, userResults, train);
        Message message = new Message(MessageType.SCORES, resultDTO);
        System.out.println("[publishScores] Sending SCORES to /topic/game/" + lobbyId);
        messagingTemplate.convertAndSend("/topic/game/" + lobbyId, message);
        System.out.println("[publishScores] SCORES sent!");

        if (freshLobby.getMaxRounds() == currentRoundNumber) {
            gameTearDown(freshLobby);
        }
    }

    public int calculateScore(Train train, double guessDistance) {
        double ldx = train.getLineDestination().getXCoordinate()
                - train.getLineOrigin().getXCoordinate();
        double ldy = train.getLineDestination().getYCoordinate()
                - train.getLineOrigin().getYCoordinate();
        double totalLineLength = Math.sqrt(Math.pow(ldx, 2) + Math.pow(ldy, 2));

        if (totalLineLength < 1.0) {
            totalLineLength = 1000.0;
        }

        double errorRatio = guessDistance / totalLineLength;
        final double p = 1.5;
        final double k = Math.log(5.0) / Math.pow(0.5, p);
        double rawScore = 1000.0 * Math.exp(-k * Math.pow(errorRatio, p));

        int finalScore = (int) Math.min(1000, Math.max(0, Math.round(rawScore)));
        double absoluteKm = guessDistance / 1000.0;
        final double lambda = 0.01;
        double dampener = Math.exp(-lambda * absoluteKm);
        finalScore = (int)(finalScore * dampener);

        return finalScore;
    }

    public double calculateGuessDistance(Train train, Long playerX, Long playerY) {
        double dx = playerX - train.getCurrentX();
        double dy = playerY - train.getCurrentY();
        return Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
    }

    public void gameTearDown(Lobby currentLobby) {
        Long lobbyId = currentLobby.getLobbyId();

        List<Round> rounds = roundRepository.findByLobbyOrderByRoundNumberAsc(currentLobby);
        for (Round round : rounds) {
            List<Guess> guesses = guessRepository.findByRound(round);
            for (Guess guess : guesses) {
                RoundHistory roundHistory = new RoundHistory();
                roundHistory.setLobby(currentLobby);
                roundHistory.setUser(guess.getUser());
                roundHistory.setRoundNumber(round.getRoundNumber());
                roundHistory.setPoints(guess.getPoints() != null ? guess.getPoints() : 0);
                roundHistory.setDistanceToTrain(guess.getDistanceToTrain() != null ? guess.getDistanceToTrain() : 0f);
                roundHistoryRepository.save(roundHistory);
            }
        }

        for (User player : currentLobby.getPlayers()) {
            Long userId = player.getUserId();
            List<RoundHistory> playerHistory = roundHistoryRepository.findByUserUserId(userId);

            UserScoreboard scoreboard = player.getUserScoreboard();
            scoreboard.setPlayedGames(scoreboard.getPlayedGames() + 1);
            scoreboard.setPlayedRounds(scoreboard.getPlayedRounds() + rounds.size());
            scoreboard.setTotalPoints(playerHistory.stream().mapToLong(r -> r.getPoints()).sum());
            scoreboard.setBestRoundPoints(playerHistory.stream().mapToLong(r -> r.getPoints()).max().orElse(0));
            scoreboard.setGuessingPrecision((float) playerHistory.stream().mapToDouble(r -> r.getDistanceToTrain()).average().orElse(0));
            player.setUserScoreboard(scoreboard);
            userRepository.save(player);
        }

        currentLobby.setLobbyState(LobbyState.FINISHED);
        lobbyRepository.save(currentLobby);

        for (Round round : rounds) {
            guessRepository.deleteByRound(round);
        }
        roundRepository.deleteByLobby(currentLobby);

        activeTimers.remove(lobbyId);
        scoresPublished.remove(lobbyId);
    }

    public void cleanupAllTimers() {
        activeTimers.forEach((gameId, timer) -> {
            if (timer != null) {
                timer.cancel(false);
            }
        });
        activeTimers.clear();
    }
}