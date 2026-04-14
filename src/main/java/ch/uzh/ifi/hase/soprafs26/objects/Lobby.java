package ch.uzh.ifi.hase.soprafs26.objects;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;
import ch.uzh.ifi.hase.soprafs26.entity.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Lobby {

    //randomized??
    private Long lobbyId;

    private String lobbyName;

    //randomized??
    private String lobbyCode;

    private Admin admin;

    private Integer size;

    private LobbyVisibility visibility;

    private List<User> users;

    private List<Round> rounds;

    private Integer currentRound;

    private Integer maxRounds;

    private Map<Long, Score> scores;

    private LobbyState lobbyState;

    private Game game;


    public Long getLobbyId() {return lobbyId;}
    public void setLobbyId(Long lobbyId) {this.lobbyId = lobbyId;}

    public String getLobbyName() {return lobbyName;}
    public void setLobbyName(String lobbyName) {this.lobbyName = lobbyName;}

    public String getLobbyCode() {return lobbyCode;}
    public void setLobbyCode(String lobbyCode) {this.lobbyCode = lobbyCode;}

    public Admin getAdmin() {return admin;}
    public void setAdmin(Admin admin) {this.admin = admin;}

    public Integer getSize() {return size;}
    public void setSize(Integer size) {this.size = size;}

    public LobbyVisibility getVisibility() {return visibility;}
    public void setVisibility(LobbyVisibility visibility) {this.visibility = visibility;}

    public List<User> getUsers() {return users;}
    public void setUsers(List<User> users) {this.users = users;}
    public void addUser(User user) {this.users.add(user);}
    public void removeUser(User user) {this.users.remove(user);}

    public List<Round> getRounds() {return rounds;}
    public void setRounds(List<Round> rounds) {this.rounds = rounds;}

    public Integer getCurrentRound() {return currentRound;}
    public void setCurrentRound(Integer currentRound) {this.currentRound = currentRound;}

    public Integer getMaxRounds() {return maxRounds;}
    public void setMaxRounds(Integer maxRounds) {this.maxRounds = maxRounds;}

    public List<Score> getScores() {return new ArrayList<>(scores.values());}
    public void setScore(long userId, Score score) {this.scores.put(userId, score);}
    public void setScores(Map<Long, Score> scores) {this.scores = scores;}
    public Score getScore(long userId) {return this.scores.get(userId);}

    public LobbyState getLobbyState()  {return lobbyState;}
    public void setLobbyState(LobbyState lobbyState) {this.lobbyState = lobbyState;}

    public Game getGame() {return game;}
    public void setGame(Game game) {this.game = game;}

}
