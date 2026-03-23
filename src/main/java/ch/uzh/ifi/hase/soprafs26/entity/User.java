package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Internal User Representation
 * This class composes the internal representation of the user and defines how
 * the user is stored in the database.
 * Every variable will be mapped into a database field with the @Column
 * annotation
 * - nullable = false -> this cannot be left empty
 * - unique = true -> this value must be unqiue across the database -> composes
 * the primary key
 */
@Entity
@Table(name = "users")
public class User implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private Long userId;

    @Column(nullable = true)
    private UserScoreboard userScoreboard;

	@Column(nullable = false, unique = true)
	private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = true)
    private String userBio;

    @Column(nullable = false)
    private Date creationDate;

	@Column(nullable = true, unique = true)
	private String token;

	@Column(nullable = false)
	private UserStatus status;

    @Column(nullable = true)
    private List<User> friends;



	public Long getUserId() {
		return userId;
	}
	public void setUserId(Long userId) {
		this.userId = userId;
	}

    public UserScoreboard getUserScoreboard() {return userScoreboard;}
    public void setUserScoreboard(UserScoreboard userScoreboard) {this.userScoreboard = userScoreboard;}

	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}

    public String getEmail() {return email;}
    public void setEmail(String email) {this.email = email;}

    public String getPassword() {return password;}
    public void setPassword(String password) {this.password = password;}

    public String getUserBio() {
        return userBio;
    }
    public void setUserBio(String userBio) {
        this.userBio = userBio;
    }

    public Date getCreationDate() {return creationDate;}
    public void setCreationDate(Date creationDate) {this.creationDate = creationDate;}

	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}

	public UserStatus getStatus() {
		return status;
	}
	public void setStatus(UserStatus status) {
		this.status = status;
	}

    public List<User> getFriends() {return friends;}
    public void setFriends(List<User> friends) {this.friends = friends;}
}
