package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.Column;

public class Admin {

    private String userId;

    private String token;

    public String getUserId() {return userId;}
    public void setUserId(String userId) {this.userId = userId;}

    public String getToken() {return token;}
    public void setToken(String token) {this.token = token;}
}
