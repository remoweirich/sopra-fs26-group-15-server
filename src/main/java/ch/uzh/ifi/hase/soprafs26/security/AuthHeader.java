package ch.uzh.ifi.hase.soprafs26.security;

public class AuthHeader {
    private Long userId;
    private String token;

    public AuthHeader() {}

    public AuthHeader(Long userId, String token) {
        this.userId = userId;
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
