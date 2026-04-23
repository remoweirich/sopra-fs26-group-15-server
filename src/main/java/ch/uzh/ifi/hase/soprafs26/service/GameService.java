package ch.uzh.ifi.hase.soprafs26.service;


import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;
import ch.uzh.ifi.hase.soprafs26.constant.MessageType;
import ch.uzh.ifi.hase.soprafs26.entity.GameResult;
import ch.uzh.ifi.hase.soprafs26.events.GameEndedEvent;
import ch.uzh.ifi.hase.soprafs26.objects.Game;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.objects.*;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessMessageDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MyLobbyDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ResultDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RoundStartDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.trains.TrainPositionFetcher;
import org.springframework.context.ApplicationEventPublisher;
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
    private final UserRepository userRepository;
    //private AuthService authService;

    private List<Game> activeGames;

    private TrainPositionFetcher trainPositionFetcher;
    private final GameRepository gameRepository;
    private final Map<Long, ScheduledFuture<?>> activeTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;

    private Map<Long, Boolean> scoresPublished = new HashMap<>();

    public GameService(/*AuthService authService,*/ TrainPositionFetcher trainPositionFetcher, GameRepository gameRepository, SimpMessagingTemplate messagingTemplate, ApplicationEventPublisher eventPublisher, UserRepository userRepository) {
        //this.authService = authService;
        this.eventPublisher = eventPublisher;
        this.trainPositionFetcher = trainPositionFetcher;
        this.gameRepository = gameRepository;
        this.messagingTemplate = messagingTemplate;
        this.activeGames = new ArrayList<>();
        this.userRepository = userRepository;
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
        Long gameId = currentLobby.getLobbyId();
        scoresPublished.put(gameId, false);

        for (int i = 0; i < currentLobby.getMaxRounds(); i++) {
            Map<Long, UserGameStatus> roundUserStatus = new HashMap<>();
            Map<Long, GuessMessageDTO> roundGuesses = new HashMap<>();
            Map<Long, Score> roundScores = new HashMap<>();
            Map<Long, Double> roundDistances = new HashMap<>();

            for (User user : players) {
                Long userId = user.getUserId();
                Score score = new Score(userId);
                roundDistances.put(userId, 0.0);
                score.setPoints(0);
                currentLobby.setScore(userId, score);
                roundUserStatus.put(userId, new UserGameStatus(userId, false));
                roundGuesses.put(userId, new GuessMessageDTO(gameId, userId));
                roundScores.put(userId, new Score(userId));
            }

            rounds.add(new Round(i+1, trains.get(i), roundGuesses, roundUserStatus, roundScores, roundDistances));
        }

        Game newGame = new Game(gameId, rounds, trains, connectedPlayers);

        activeGames.add(newGame);

        MyLobbyDTO myLobbyDTO = DTOMapper.INSTANCE.convertEntityToMyLobbyDTO(currentLobby);
        Message message = new Message(MessageType.GAME_START, myLobbyDTO);
        messagingTemplate.convertAndSend("/topic/lobby/" + currentLobby.getLobbyId(), message);

        return newGame;

        //DoneTODO: call Round start, allow for Frontend to subscribe to Round start messages, and trigger the timer for the first round

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
        
        System.out.println("received guess message for game " + gameId + " from user " + userId + " with coordinates: " + guessMessage.getXcoordinate() + ", " + guessMessage.getYcoordinate());

        if (!canSubmitGuess(gameId)){
            System.out.println("User " + userId + " submitted guess too late - round already ended");
            return;
        }

        // Ensure the lobby has the game attached (in case a fresh lobby was retrieved from DB)
        if (currentLobby.getGame() == null) {
            currentLobby.setGame(getGameById(gameId));
        }

        Game currentGame = currentLobby.getGame();
        int roundNumber = currentLobby.getCurrentRound() -1;
        List<Round> rounds = currentGame.getRounds();

        if (roundNumber < 0 || roundNumber >= rounds.size()) {
            System.out.println("Invalid round number: " + roundNumber);
            return;
        }

        Round currentRound = rounds.get(roundNumber);
        Train currentTrain = currentRound.getTrain();
        Long playerGuessX = guessMessage.getXcoordinate();
        Long playerGuessY = guessMessage.getYcoordinate();

        double guessDistance = calculateGuessDistance(currentTrain, playerGuessX, playerGuessY);
        int points = calculateScore(currentTrain, guessDistance);

        currentRound.setScore(userId, points);
        currentRound.setGuessMessage(userId, guessMessage);
        double roundedDistanceKm = Math.round((guessDistance / 1000.0) * 100.0) / 100.0;
        currentRound.setDistances(userId, roundedDistanceKm);

        System.out.println("Processed guess for user " + userId + " in round " + (roundNumber + 1) + ": distance=" + guessDistance + ", points=" + points);

        updateLobbyTotalScore(currentLobby, userId, points);

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
        Score score = lobby.getScore(userId);
        score.setPoints(score.getPoints() + pointsToAdd);
        lobby.setScore(userId, score);
    }

    public void readyForNextRound(UserGameStatus userGameStatus, Lobby currentLobby){
        // Ensure the lobby has the game attached (in case a fresh lobby was retrieved from DB)
        if (currentLobby.getGame() == null) {
            currentLobby.setGame(getGameById(currentLobby.getLobbyId()));
        }
        
        Boolean allAreReady = updateUserGameStatus(userGameStatus, currentLobby);
        
        if (allAreReady) {
            System.out.println("all users ready, roundStart()");
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
            int totalUsersInLobby = currentLobby.getUsers().size();
            
            // Check if all users in the lobby have connected
            if (connectedPlayers.size() != totalUsersInLobby) {
                System.out.println("Not all users connected yet. Connected: " + connectedPlayers.size() + "/" + totalUsersInLobby);
                return false;
            }
            
            // Check if all connected players are ready
            for (UserGameStatus connectedPlayer : connectedPlayers) {
                if (connectedPlayer.getIsReady() == false) {
                    System.out.println("User " + connectedPlayer.getUserId() + " is not ready");
                    return false;
                }
            }
            System.out.println("All users ready! Total: " + totalUsersInLobby);
            return true;
        }
        Round currentRound =  rounds.get(currentRoundNumber-1);
        
        currentRound.setUserGameStatus(userGameStatus.getUserId(), userGameStatus.getIsReady());
        
        List<UserGameStatus> allUsersGameStatuses = currentRound.getAllUserGameStatusesList();

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
        int currentRoundNumber =  currentLobby.getCurrentRound()+1;
        currentLobby.setCurrentRound(currentRoundNumber);

        Game currentGame = currentLobby.getGame();
        Long gameId = currentGame.getGameId();
        scoresPublished.put(gameId, false);

        Train trainWithoutCoordinates = new Train(currentGame.getTrains().get(currentRoundNumber-1));
        trainWithoutCoordinates.setCurrentX(0);
        trainWithoutCoordinates.setCurrentY(0);

        int maxRounds = currentLobby.getMaxRounds();

        RoundStartDTO roundStartDTO = new RoundStartDTO(currentRoundNumber, maxRounds, trainWithoutCoordinates);
        Message message = new Message(MessageType.ROUND_START, roundStartDTO);
        messagingTemplate.convertAndSend("/topic/game/" + gameId, message);
        System.out.println("sent round start message for game " + gameId + " and round " + currentRoundNumber + message);

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
        if (!scoresPublished.get(currentLobby.getLobbyId())) {
            publishScores(currentLobby);
        }
    }

    public void publishScores(Lobby currentLobby) {
        activeTimers.remove(currentLobby.getLobbyId());
        Game currentGame =  currentLobby.getGame();
        Long gameId = currentGame.getGameId();
        scoresPublished.put(gameId, true);
        int currentRoundNumber = currentLobby.getCurrentRound();
        Train train = currentGame.getTrains().get(currentRoundNumber-1);
        Round currentRound =  currentGame.getRounds().get(currentRoundNumber-1);

        List<Score> totalScores =  currentLobby.getScores();

        Map<Long, Score> roundScores = currentRound.getScores();
        Map<Long, Double> distances = currentRound.getDistances();
        List<UserResult> userResults = new ArrayList<>();

        for (Score totalScore : totalScores) {
            long userId = totalScore.getUserId();
            int totalPoints = totalScore.getPoints();
            Integer roundPoints = roundScores.get(userId).getPoints();
            if(roundPoints == null){
                roundPoints = 0;
            }
            GuessMessageDTO guessMessage = currentRound.getGuessMessages().get(userId);
            long xCoordinate = 0;
            long yCoordinate = 0;
            if (guessMessage != null && guessMessage.getXcoordinate() != null && guessMessage.getYcoordinate() != null) {
                xCoordinate = guessMessage.getXcoordinate().longValue();
                yCoordinate = guessMessage.getYcoordinate().longValue();
            }
            double distance = distances.get(userId);
            if (distance == 0.0 && guessMessage == null) {
                distance = Double.MAX_VALUE; // User didn't submit a guess
            }
            userResults.add(new UserResult(userId, totalPoints, roundPoints, xCoordinate, yCoordinate, distance));
        }


        ResultDTO resultDTO = new ResultDTO(currentRoundNumber, userResults, train);
        Message message = new Message(MessageType.SCORES, resultDTO);
        messagingTemplate.convertAndSend("/topic/game/" + gameId, message);


        if (currentLobby.getMaxRounds() == currentRoundNumber){
            System.out.println("call GameTearDown for game  " + gameId);
            gameTearDown(currentLobby);
        }

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
     *   errorRatio = 0.10 →  697 pts  (great)
     *   errorRatio = 0.25 →  283 pts  (decent)
     *   errorRatio = 0.50 →   200 pts  (poor)
     *   errorRatio = 1.00 →    80 pts  (terrible)
     */
    public int calculateScore(Train train, double guessDistance) {
        // 1. Total length of the train line (origin → destination)
        double ldx = train.getLineDestination().getXCoordinate()
                - train.getLineOrigin().getXCoordinate();
        double ldy = train.getLineDestination().getYCoordinate()
                - train.getLineOrigin().getYCoordinate();
        double totalLineLength = Math.sqrt(Math.pow(ldx, 2) + Math.pow(ldy, 2));

        System.out.println("calculateScore: origin=(" + train.getLineOrigin().getXCoordinate() + "," + train.getLineOrigin().getYCoordinate() + 
                           "), dest=(" + train.getLineDestination().getXCoordinate() + "," + train.getLineDestination().getYCoordinate() +
                           "), lineLength=" + totalLineLength + ", guessDistance=" + guessDistance);

        // Edge case: degenerate line (origin == destination)
        // Fall back to a fixed reference distance of 1 km in EPSG:3857 meters
        if (totalLineLength < 1.0) {
            totalLineLength = 1000.0;
            System.out.println("Using fallback line length: 1000");
        }

        // 2. Relative error ratio (clamped — can't do worse than a full line length)
        double errorRatio = guessDistance / totalLineLength;

        // 3. Power-modified exponential decay
        //    p controls curve shape: lower p = steeper near 0, flatter tail
        //    k is anchored so that errorRatio = 0.5 → exactly 100 pts
        //    k = ln(10) / 0.5^p
        final double p = 1.5;
        final double k = Math.log(5.0) / Math.pow(0.5, p);
        double rawScore = 1000.0 * Math.exp(-k * Math.pow(errorRatio, p));
        System.out.println("errorRatio=" + errorRatio + ", p=" + p + ", k=" + k + ", rawScore=" + rawScore);

        // 4. Round and clamp to [0, 1000]
        int finalScore = (int) Math.min(1000, Math.max(0, Math.round(rawScore)));
        System.out.println("finalScore=" + finalScore);
        return finalScore;
    }

    /**
     * Helper method to calculate the distance between the player's guess and the train's actual position.
     */
    public double calculateGuessDistance(Train train, Long playerX, Long playerY) {
        double dx = playerX - train.getCurrentX();
        double dy = playerY - train.getCurrentY();
        return Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
    }

    public void gameTearDown(Lobby currentLobby) {
        List<Score> currentScores = currentLobby.getScores();
        Game game = currentLobby.getGame();
        List<Round> rounds = game.getRounds();
        Long gameId = game.getGameId();
        GameResult gameResult = gameRepository.findByGameId(gameId);
        gameResult.setRounds(rounds);
        gameResult.setScores(currentScores);
        Map<Long, String> usernames = new HashMap<>();
        for (Score score : currentScores) {
            long userId = score.getUserId();
            String username = userRepository.findById(score.getUserId()).get().getUsername();
            usernames.put(userId, username);
        }
        gameResult.setUsernames(usernames);

        gameRepository.save(gameResult);
        gameRepository.flush();
        activeTimers.remove(gameId);
        scoresPublished.remove(gameId);

        eventPublisher.publishEvent(new GameEndedEvent(this, gameId));


        activeGames.remove(game);
        currentLobby.setGame(null);
        System.out.println("Game " + game.getGameId() + " has ended and been removed from active games.");

    }
}
