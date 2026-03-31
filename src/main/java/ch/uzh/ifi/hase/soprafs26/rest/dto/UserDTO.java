package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.UserScoreboard;

import java.util.Date;
import java.util.List;

public class UserDTO {

    private String username;

    private String userBio;

    private UserScoreboard userScoreboard;

    private List<User> friends;

    private Date creationDate;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserBio() {
        return userBio;
    }

    public void setUserBio(String userBio) {
        this.userBio = userBio;
    }

    public UserScoreboard getUserScoreboard() {
        return userScoreboard;
    }

    public void setUserScoreboard(UserScoreboard userScoreboard) {
        this.userScoreboard = userScoreboard;
    }

    public List<User> getFriends() {
        return friends;
    }

    public void setFriends(List<User> friends) {
        this.friends = friends;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

}
