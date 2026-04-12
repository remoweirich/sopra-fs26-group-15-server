package ch.uzh.ifi.hase.soprafs26.service;


import ch.uzh.ifi.hase.soprafs26.constant.MessageType;
import ch.uzh.ifi.hase.soprafs26.objects.Game;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.objects.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessMessageDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MyLobbyDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ResultDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RoundStartDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.security.AuthService;
import ch.uzh.ifi.hase.soprafs26.trains.TrainPositionFetcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import ch.uzh.ifi.hase.soprafs26.websocket.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;


@Service
@Transactional
public class GameService {
    private AuthService authService;

    private List<Game> activeGames;

    private TrainPositionFetcher trainPositionFetcher;

    private final Map<Long, ScheduledFuture<?>> activeTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    private final SimpMessagingTemplate messagingTemplate;

    private Boolean scoresPublished = false;

    public GameService(AuthService authService, TrainPositionFetcher trainPositionFetcher, SimpMessagingTemplate messagingTemplate) {
        this.authService = authService;
        this.trainPositionFetcher = trainPositionFetcher;
        this.messagingTemplate = messagingTemplate;
        this.activeGames = new ArrayList<>();
    }

    public Game getGameById(Long gameId) {
        for (Game game : activeGames) {
            if (game.getGameId().equals(gameId)) {
                return game;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");
    }

    public Game setupGame(Lobby currentLobby) {

        try {
            List<Train> trains = trainPositionFetcher.fetchTrainsMock(currentLobby.getMaxRounds());

            for (Train train : trains) {
                trainPositionFetcher.interpolatePosition(train);
            }

        List<Round> rounds = new ArrayList<>();

        List<User> players = currentLobby.getUsers();

        Map<Long, UserGameStatus> connectedPlayers = new HashMap<>();

        for (int i = 0; i < currentLobby.getMaxRounds(); i++) {
            Map<Long, UserGameStatus> roundUserStatus = new HashMap<>();
            Map<Long, GuessMessageDTO> roundGuesses = new HashMap<>();
            Map<Long, Score> roundScores = new HashMap<>();

            for (User user : players) {
                Long userId = user.getUserId();
                roundUserStatus.put(userId, new UserGameStatus(userId, false));
                roundGuesses.put(userId, new GuessMessageDTO(currentLobby.getLobbyId(), userId));
                roundScores.put(userId, new Score(userId));
            }

            rounds.add(new Round(i+1, trains.get(i), roundGuesses, roundUserStatus, roundScores));
        }

        Game newGame = new Game(currentLobby.getLobbyId(), rounds, trains, connectedPlayers);

        activeGames.add(newGame);

        MyLobbyDTO myLobbyDTO = DTOMapper.INSTANCE.convertEntityToMyLobbyDTO(currentLobby);
        Message message = new Message(MessageType.GAME_START, myLobbyDTO);
        messagingTemplate.convertAndSend("/topic/lobby/" + currentLobby.getLobbyId(), message);

        return newGame;

        //TODO: call Round start, allow for Frontend to subscribe to Round start messages, and trigger the timer for the first round

        } catch (Exception e) {
            throw new Error("Failed to fetch mock trains", e);
        }
    }



    /**
     * Process Player guess, save score and send back userGameStatus to frontend subscribers
     */
    public void processGuessMessage(GuessMessageDTO guessMessage, Lobby currentLobby){
        Long gameId = guessMessage.getLobbyId();
        Long userId = guessMessage.getUserId();

        if (!canSubmitGuess(gameId)){
            return;
        }

        Game currentGame = getGameById(gameId);
        int roundNumber = currentLobby.getCurrentRound() -1;
        List<Round> rounds = currentGame.getRounds();

        Round currentRound = rounds.get(roundNumber);
        Train currentTrain = currentRound.getTrain();
        Long playerGuessX = guessMessage.getXcoordinate();
        Long playerGuessY = guessMessage.getYcoordinate();

        int score = calculateScore(currentTrain, playerGuessX, playerGuessY);
        double distance = calculateGuessDistance(currentTrain, playerGuessX, playerGuessY);

        currentRound.setScore(userId, score);
        currentRound.setGuessMessage(userId, guessMessage);

        updateLobbyTotalScore(currentLobby, userId, score);

        UserGameStatus userGameStatus = new UserGameStatus(userId, true);
        Boolean allAreReady = updateUserGameStatus(userGameStatus, currentLobby);

        if (allAreReady) {
            ScheduledFuture<?> timer = activeTimers.get(gameId);
            if (timer != null){
                timer.cancel(false);
            }
            allowedToPublish(currentLobby);
        }
        Message message = new Message(MessageType.GAME_STATE, userId);
        messagingTemplate.convertAndSend("/topic/game/" + gameId, message);


    }

    private void updateLobbyTotalScore(Lobby lobby, Long userId, int pointsToAdd) {
        List<Score> totalScores = lobby.getScores();

        for (Score s : totalScores) {
            if (s.getUserId().equals(userId)) {
                // Assuming Score class has getScore() and setScore()
                int currentTotal = s.getPoints();
                s.setPoints(currentTotal + pointsToAdd);
                lobby.setScores(totalScores);
                return;
            }
        }

        // Fallback: If for some reason the score object doesn't exist yet, create it
        Score newScore = new Score(userId);
        newScore.setPoints(pointsToAdd);
        totalScores.add(newScore);
        lobby.setScores(totalScores);
    }

    public void readyForNextRound(UserGameStatus userGameStatus, Lobby currentLobby){
        Boolean allAreReady = updateUserGameStatus(userGameStatus, currentLobby);
        if (allAreReady) {
            roundStart(currentLobby);
        }
    }

    public Boolean updateUserGameStatus(UserGameStatus userGameStatus, Lobby currentLobby) {
        Game currentGame = currentLobby.getGame();
        List<Round> rounds = currentGame.getRounds();
        int currentRoundNumber = currentLobby.getCurrentRound();
        if (currentRoundNumber == 0){
            currentGame.setConnectedPlayers(userGameStatus.getUserId(), userGameStatus);
            List<UserGameStatus> connectedPlayers = currentGame.getConnectedPlayers();
            for (UserGameStatus connectedPlayer : connectedPlayers) {
                if (connectedPlayer.getIsReady() == false) {
                    return false;
                }
            }
            return true;

        }
        Round currentRound =  rounds.get(currentRoundNumber-1);
        List<UserGameStatus> allUsersGameStatuses = currentRound.getAllUserGameStatuses();

        currentRound.setUserGameStatus(userGameStatus.getUserId(), userGameStatus.getIsReady());

        for (UserGameStatus usGaSt : allUsersGameStatuses) {
            if (usGaSt.getIsReady() == false) {
                return false;
            }
        }
        return true;
    }

    public boolean canSubmitGuess(long gameId) {
        return activeTimers.containsKey(gameId);
    }

    public void roundStart(Lobby currentLobby) {
        scoresPublished = false;
        int currentRoundNumber =  currentLobby.getCurrentRound()+1;
        currentLobby.setCurrentRound(currentRoundNumber);

        Game currentGame = currentLobby.getGame();
        Long gameId = currentGame.getGameId();

        Train trainWithoutCoordinates = currentGame.getTrains().get(currentRoundNumber-1);
        trainWithoutCoordinates.setCurrentX(0);
        trainWithoutCoordinates.setCurrentY(0);

        int maxRounds = currentLobby.getMaxRounds();

        RoundStartDTO roundStartDTO = new RoundStartDTO(currentRoundNumber, maxRounds, trainWithoutCoordinates);
        Message message = new Message(MessageType.ROUND_START, roundStartDTO);
        messagingTemplate.convertAndSend("/topic/game/" + gameId, message);

        ScheduledFuture<?> timer = scheduler.schedule(
                () -> roundEnd(currentLobby),
                45, //might be longer depending on front end timer implementation
                TimeUnit.SECONDS
        );

        activeTimers.put(gameId, timer);
    }

    public void roundEnd(Lobby currentLobby) {
        Long gameId = currentLobby.getLobbyId();

        messagingTemplate.convertAndSend("/topic/game/"+ gameId,
                new Message(MessageType.ROUND_END, null));

        ScheduledFuture<?> lastMessagesTimer = scheduler.schedule(
                () -> allowedToPublish(currentLobby),
                3,
                TimeUnit.SECONDS
        );

        activeTimers.put(gameId, lastMessagesTimer);
    }

    public void allowedToPublish(Lobby currentLobby) {
        if (!scoresPublished) {
            publishScores(currentLobby);
        }
    }

    public void publishScores(Lobby currentLobby) {
        scoresPublished = true;
        Game currentGame =  currentLobby.getGame();
        int currentRoundNumber = currentLobby.getCurrentRound();
        Train train = currentGame.getTrains().get(currentRoundNumber-1);
        Round currentRound =  currentGame.getRounds().get(currentRoundNumber-1);

        List<Score> totalScores =  currentLobby.getScores();

        Map<Long, Score> roundScores = currentGame.getRounds().get(currentLobby.getCurrentRound()).getAllScores();
        Map<Long, Double> distances = currentRound.getDistances();
        List<UserResult> userResults = new ArrayList<>();

        for (Score totalScore : totalScores) {
            long userId = totalScore.getUserId();
            int totalPoints = totalScore.getPoints();
            int roundPoints = roundScores.get(userId).getPoints();
            GuessMessageDTO guessMessage = currentRound.getGuessMessages().get(userId);
            long xCoordinate = guessMessage.getXcoordinate();
            long yCoordinate = guessMessage.getYcoordinate();
            double distance = distances.get(userId);
            userResults.add(new UserResult(userId, totalPoints, roundPoints, xCoordinate, yCoordinate, distance));
        }

        ResultDTO resultDTO = new ResultDTO(currentRoundNumber, userResults, train);
        Message message = new Message(MessageType.SCORES, resultDTO);
        messagingTemplate.convertAndSend("/topic/game/" + currentGame.getGameId(), message);


    }

    /**
     * Calculates a score (0–1000) for a player's train position guess.
     *
     * Uses Gaussian decay: score = 1000 * e^(-k * errorRatio²)
     * where errorRatio = guessDistance / totalLineLength
     *
     * Decay constant k is chosen so that errorRatio = 1.0 (guess is off by a full
     * line length) yields a score of ~5, giving a near-zero floor for bad guesses.
     *
     *   errorRatio = 0.00 → 1000 pts  (perfect)
     *   errorRatio = 0.10 →  742 pts  (great)
     *   errorRatio = 0.25 →  214 pts  (decent)
     *   errorRatio = 0.50 →   21 pts  (poor)
     *   errorRatio = 1.00 →    5 pts  (terrible)
     */
    public int calculateScore(Train train, long playerX, long playerY) {

        // 1. Euclidean distance between the guess and the train's actual position
        double dx = playerX - train.getCurrentX();
        double dy = playerY - train.getCurrentY();
        double guessDistance = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));

        // 2. Total length of the train line (origin → destination)
        double ldx = train.getLineDestination().getXCoordinate()
                - train.getLineOrigin().getXCoordinate();
        double ldy = train.getLineDestination().getYCoordinate()
                - train.getLineOrigin().getYCoordinate();
        double totalLineLength = Math.sqrt(Math.pow(ldx, 2) + Math.pow(ldy, 2));

        // Edge case: degenerate line (origin == destination)
        // Fall back to a fixed reference distance of 1 km in EPSG:3857 meters
        if (totalLineLength < 1.0) {
            totalLineLength = 1000.0;
        }

        // 3. Relative error ratio (clamped — can't do worse than a full line length)
        double errorRatio = guessDistance / totalLineLength;

        // 4. Gaussian decay: k = ln(1000/5) ≈ 5.298
        //    Chosen so that errorRatio = 1.0 → score ≈ 5 (near-zero, not exactly 0)
        final double k = Math.log(1000.0 / 5.0); // ≈ 5.298

        double rawScore = 1000.0 * Math.exp(-k * Math.pow(errorRatio, 2));

        // 5. Round and clamp to [0, 1000]
        return (int) Math.min(1000, Math.max(0, Math.round(rawScore)));
    }

    /**
     * Helper method to calculate the distance between the player's guess and the train's actual position.
     */
    public double calculateGuessDistance(Train train, Long playerX, Long playerY) {
        double dx = playerX - train.getCurrentX();
        double dy = playerY - train.getCurrentY();
        double guessDistance = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
        return Math.round(guessDistance / 1000.0);
    }
}
