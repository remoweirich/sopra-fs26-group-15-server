package ch.uzh.ifi.hase.soprafs26.service;


import ch.uzh.ifi.hase.soprafs26.constant.MessageType;
import ch.uzh.ifi.hase.soprafs26.objects.Game;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.objects.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessMessageDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MyLobbyDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.security.AuthService;
import ch.uzh.ifi.hase.soprafs26.trains.TrainPositionFetcher;
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


@Service
@Transactional
public class GameService {
    private AuthService authService;

    private List<Game> activeGames;

    private TrainPositionFetcher trainPositionFetcher;

    private final SimpMessagingTemplate messagingTemplate;

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

        Map<Long, UserGameStatus> allUsersGameStatus = new HashMap<>();
        Map<Long, GuessMessageDTO> guessMessages = new HashMap<>();
        Map<Long, Score> scores = new HashMap<>();

        for  (User user : players) {
            Long userId = user.getUserId();
            scores.put(userId, new Score(user.getUserId()));
            allUsersGameStatus.put(userId, new UserGameStatus(user.getUserId(), false));
            guessMessages.put(userId, new GuessMessageDTO(currentLobby.getLobbyId(), user.getUserId()));
        }

        for (int i = 0; i < currentLobby.getMaxRounds(); i++) {
            rounds.add(new Round(i+1, trains.get(i), guessMessages, allUsersGameStatus, scores));
        }

        Game newGame = new Game(currentLobby.getLobbyId(), rounds, trains);

        activeGames.add(newGame);

        MyLobbyDTO myLobbyDTO = DTOMapper.INSTANCE.convertEntityToMyLobbyDTO(currentLobby);
        Message message = new Message(MessageType.GAME_START, myLobbyDTO);
        messagingTemplate.convertAndSend("/topic/lobby/" + currentLobby.getLobbyId(), message);

        return newGame;

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

        Game currentGame = getGameById(gameId);
        Integer roundNumber = currentLobby.getCurrentRound();
        List<Round> rounds = currentGame.getRounds();

        Round currentRound = rounds.get(roundNumber - 1);
        Train currentTrain = currentRound.getTrain();
        Long playerGuessX = guessMessage.getXcoordinate();
        Long playerGuessY = guessMessage.getYcoordinate();

        int score = calculateScore(currentTrain, playerGuessX, playerGuessY);
        double distance = calculateGuessDistance(currentTrain, playerGuessX, playerGuessY);

        currentRound.setScore(userId, score);
        currentRound.setGuessMessage(userId, guessMessage);
        // TODO : Send Score back to frontend subscribers
        UserGameStatus userGameStatus = new UserGameStatus(userId, true);
        updateUserGameStatus(userGameStatus, currentLobby);

        Message message = new Message(MessageType.GAME_STATE, currentGame);
        messagingTemplate.convertAndSend("/topic/game/" + gameId, message);


    }

    public void updateUserGameStatus(UserGameStatus userGameStatus, Lobby currentLobby) {
        Game currentGame = currentLobby.getGame();
        List<Round> rounds = currentGame.getRounds();
        Round currentRound =  rounds.get(currentLobby.getCurrentRound());

        currentRound.setUserStatus(userGameStatus.getUserId(), userGameStatus.getIsReady());

    }

    public void publishRoundStart(){

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
        return Math.round(guessDistance * 1000.0);
    }
}
