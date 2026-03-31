package ch.uzh.ifi.hase.soprafs26.service;


import ch.uzh.ifi.hase.soprafs26.objects.Game;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.objects.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessMessageDTO;
import ch.uzh.ifi.hase.soprafs26.security.AuthService;

import java.util.ArrayList;
import java.util.List;

public class GameService {
    private AuthService authService;

    private List<Game> activeGames;

    private LobbyService lobbyService;

    private GameTrainsService gameTrainsService;

    public Game setupGame(Long lobbyId, List<User> users, Integer maxRounds) {
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

        for (Integer i = 0; i < maxRounds; i++) {
            rounds.add(new Round(i+1, trains.get(i), guessMessages, allUsersGameStatus));
        }

        Game newGame = new Game(currentLobby.getLobbyId(), rounds, trains);

        activeGames.add(newGame);

        return newGame;
    }

    public void proccessGuessMessage(GuessMessageDTO guessMessageDTO){
        Long currentGameId = guessMessageDTO.getLobbyId();
    }

    public void updateReadyStatus(Long gameId, Long userId) {

    }

}
