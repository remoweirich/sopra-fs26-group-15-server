package ch.uzh.ifi.hase.soprafs26.service;


import ch.uzh.ifi.hase.soprafs26.objects.Game;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.objects.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessMessageDTO;
import ch.uzh.ifi.hase.soprafs26.websocket.Message;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class GameService {
    private final List<Game> activeGames = new ArrayList<>();

    public Game getGameById(Long gameId) {
        for (Game game : activeGames) {
            if (game.getGameId().equals(gameId)) {
                return game;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");
    }

    public Game setupGame(Lobby lobby, Integer maxRounds) {
        if (lobby == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lobby is required");
        }
        if (maxRounds == null || maxRounds <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxRounds must be > 0");
        }
        List<Train> trains = new ArrayList<>();
        List<Round> rounds = new ArrayList<>();
        List<User> players = lobby.getUsers();
        if (players == null || players.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lobby must contain at least one player");
        }

        for (int i = 0; i < maxRounds; i++) {
            trains.add(new Train("train-" + (i + 1)));
            rounds.add(new Round(i + 1, trains.get(i), new ArrayList<>(), createUserStatuses(players)));
        }

        Game newGame = new Game(lobby.getLobbyId(), rounds, trains);

        activeGames.add(newGame);

        return newGame;
    }

    public void processGuessMessage(Long gameId, Message guessMessage){
        GuessMessageDTO guessMessageDTO = toGuessMessageDTO(guessMessage.getPayload());
        if (guessMessageDTO.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        Game currentGame = getGameById(gameId);
        Integer roundNumber = getCurrentRoundIndex(currentGame);
        currentGame.getRounds().get(roundNumber).getGuessMessages().add(guessMessageDTO);
        updateUserGameStatus(gameId, guessMessageDTO.getUserId(), roundNumber);

    }

    public void updateUserGameStatus(Long gameId, Message readyForNextRoundMessage) {
        GuessMessageDTO readyMessageDTO = toGuessMessageDTO(readyForNextRoundMessage.getPayload());
        if (readyMessageDTO.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        updateUserGameStatus(gameId, readyMessageDTO.getUserId(), getCurrentRoundIndex(getGameById(gameId)));

    }

    public void updateUserGameStatus(Long gameId, Long userId, Integer roundNumber) {
        Game currentGame = getGameById(gameId);
        List<Round> rounds = currentGame.getRounds();
        if (roundNumber < 0 || roundNumber >= rounds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid round number");
        }
        Round currentRound =  rounds.get(roundNumber);
        List<UserGameStatus> allUsersGameStatus = currentRound.getAllUserGameStatuses();
        boolean userFound = false;

        for  (UserGameStatus userGameStatus : allUsersGameStatus) {
            if(userGameStatus.getUserId().equals(userId)) {
                userGameStatus.setIsReady(true);
                userFound = true;
            }
        }

        if (!userFound) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not part of this game");
        }

    }

    private Integer getCurrentRoundIndex(Game game) {
        List<Round> rounds = game.getRounds();
        for (int i = 0; i < rounds.size(); i++) {
            boolean everyoneReady = rounds.get(i).getAllUserGameStatuses().stream()
                    .allMatch(UserGameStatus::getIsReady);
            if (!everyoneReady) {
                return i;
            }
        }
        return Math.max(rounds.size() - 1, 0);
    }

    private List<UserGameStatus> createUserStatuses(List<User> players) {
        List<UserGameStatus> statuses = new ArrayList<>();
        for (User user : players) {
            statuses.add(new UserGameStatus(user.getUserId()));
        }
        return statuses;
    }

    private GuessMessageDTO toGuessMessageDTO(Object payload) {
        if (payload instanceof GuessMessageDTO dto) {
            return dto;
        }
        if (payload instanceof GuessMessage guessMessage) {
            GuessMessageDTO dto = new GuessMessageDTO(guessMessage.getLobbyId(), guessMessage.getUserId());
            dto.setLatitude(guessMessage.getLatitude());
            dto.setLongitude(guessMessage.getLongitude());
            return dto;
        }
        if (payload instanceof Map<?, ?> mapPayload) {
            GuessMessageDTO dto = new GuessMessageDTO(0L, 0L);
            dto.setLobbyId(asLong(mapPayload.get("lobbyId")));
            dto.setUserId(asLong(mapPayload.get("userId")));
            Object latitude = mapPayload.get("latitude");
            Object longitude = mapPayload.get("longitude");
            dto.setLatitude(latitude != null ? latitude.toString() : null);
            dto.setLongitude(longitude != null ? longitude.toString() : null);
            return dto;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported message payload");
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        }
        catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid numeric value in payload");
        }
    }

}
