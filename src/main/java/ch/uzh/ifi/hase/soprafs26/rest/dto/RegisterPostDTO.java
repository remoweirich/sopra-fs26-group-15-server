package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class RegisterPostDTO {
    private String username;

    private String email;

    private String password;

    private String userBio;


    public String getUsername() {return username;}
    public void setUsername(String username) {this.username = username;}

    public String getEmail() {return email;}
    public void setEmail(String email) {this.email = email;}

    public String getPassword() {return password;}
    public void setPassword(String password) {this.password = password;}

    public String getUserBio() {return userBio;}
    public void setUserBio(String userBio) {this.userBio = userBio;}
}
