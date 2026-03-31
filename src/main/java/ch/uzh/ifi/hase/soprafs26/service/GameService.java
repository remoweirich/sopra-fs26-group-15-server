package ch.uzh.ifi.hase.soprafs26.service;


import ch.uzh.ifi.hase.soprafs26.objects.Game;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.objects.*;
import ch.uzh.ifi.hase.soprafs26.security.AuthService;
import ch.uzh.ifi.hase.soprafs26.websocket.Message;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class GameService {
    private AuthService authService;

    private List<Game> activeGames;

    private LobbyService lobbyService;

    private GameTrainsService gameTrainsService;

    public Game getGameById(Long gameId) {
        for (Game game : activeGames) {
            if (game.getGameId().equals(gameId)) {
                return game;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");
    }

    public Game setupGame(Long lobbyId, Integer maxRounds) {
        List<Train> trains = new ArrayList<>(); //replace with fetchTrains
        List<Round> rounds = new ArrayList<>();

        Lobby currentLobby = lobbyService.getLobbyById(lobbyId);
        List<User> players = currentLobby.getUsers();

        List<UserGameStatus> allUsersGameStatus = new ArrayList<>();
        List<GuessMessage> guessMessages = new ArrayList<>();

        for  (User user : players) {
            allUsersGameStatus.add(new UserGameStatus(user.getUserId()));
            guessMessages.add(new GuessMessage(currentLobby.getLobbyId(), user.getUserId()));
        }

        for (int i = 0; i < maxRounds; i++) {
            rounds.add(new Round(i+1, trains.get(i), guessMessages, allUsersGameStatus));
        }

        Game newGame = new Game(currentLobby.getLobbyId(), rounds, trains);

        activeGames.add(newGame);

        return newGame;
    }

    public void processGuessMessage(Message guessMessage){
//        Long gameId = guessMessage.getLobbyId();
        Long gameId = guessMessage.getPayload().gameId;
        Long userId = guessMessage.getUserId();

        Lobby currentLobby = lobbyService.getLobbyById(gameId);
        Game currentGame = getGameById(gameId);
        Integer roundNumber = currentLobby.getCurrentRound();
        List<Round> rounds = currentGame.getRounds();



        updateUserGameStatus(gameId, userId, roundNumber);

    }

    public void updateUserGameStatus(Long gameId, Long userId, Integer roundNumber) {
        Game currentGame = getGameById(gameId);
        List<Round> rounds = currentGame.getRounds();
        Round currentRound =  rounds.get(roundNumber);
        List<UserGameStatus> allUsersGameStatus = currentRound.getAllUserGameStatuses();

        for  (UserGameStatus userGameStatus : allUsersGameStatus) {
            if(userGameStatus.getUserId().equals(userId)) {
                userGameStatus.setIsReady(true);
            }
        }

    }

}
