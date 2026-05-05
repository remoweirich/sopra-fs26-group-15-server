package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs26.entity.UserScoreboard;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UpdateUserPutDTO;
import ch.uzh.ifi.hase.soprafs26.security.AuthHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.entity.User;

import java.util.UUID;

@Service
@Transactional
public class UserService {

	private final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}


    public User registerUser(User newUser) {

        // Scoreboard initialisieren
        UserScoreboard userScoreboard = new UserScoreboard();
        userScoreboard.setTotalPoints(0L);
        userScoreboard.setPlayedGames(0L);
        userScoreboard.setPlayedRounds(0L);
        userScoreboard.setBestRoundPoints(0L);
        userScoreboard.setGuessingPrecision(0f);
        newUser.setUserScoreboard(userScoreboard);

        // Status setzen
        newUser.setIsOnline(false);
        newUser.setIsGuest(false);

        // creationDate wird automatisch via @PrePersist gesetzt

        // Uniqueness prüfen
        checkIfUserExists(newUser);

        // Speichern
        newUser = userRepository.save(newUser);
        userRepository.flush();

        log.debug("Created Information for User: {}", newUser);
        return newUser;
    }

    public User loginUser(String username, String password) {
        User loggedInUser = userRepository.findByUserProfileUsername(username);

        if (loggedInUser == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This user could not be found");
        }
        if (!loggedInUser.getUserProfile().getPassword().equals(password)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The credentials are wrong");
        }

        // Token generieren
        String newToken;
        do {
            newToken = UUID.randomUUID().toString();
        } while (userRepository.findByToken(newToken) != null);

        loggedInUser.setToken(newToken);
        loggedInUser.setIsOnline(true);

        loggedInUser = userRepository.save(loggedInUser);
        userRepository.flush();

        return loggedInUser;
    }



    public void logoutUser(AuthHeader authHeader) {
        User user = userRepository.findById(authHeader.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setToken(null);
        user.setIsOnline(false);

        userRepository.save(user);
        userRepository.flush();
    }

    public void updateUser(Long userId, UpdateUserPutDTO userUpdate) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (userUpdate.getUsername() != null) user.getUserProfile().setUsername(userUpdate.getUsername());
        if (userUpdate.getPassword() != null) user.getUserProfile().setPassword(userUpdate.getPassword());
        if (userUpdate.getEmail() != null) user.getUserProfile().setEmail(userUpdate.getEmail());
        if (userUpdate.getUserBio() != null) user.getUserProfile().setUserBio(userUpdate.getUserBio());

        userRepository.save(user);
        userRepository.flush();
    }

    private void checkIfUserExists(User userToBeCreated) {
        User userByUsername = userRepository.findByUserProfileUsername(
                userToBeCreated.getUserProfile().getUsername());
        User userByEmail = userRepository.findByUserProfileEmail(
                userToBeCreated.getUserProfile().getEmail());

        String baseErrorMessage = "The %s provided %s not unique. Therefore, the user could not be created!";
        if (userByUsername != null && userByEmail != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format(baseErrorMessage, "username and the email", "are"));
        } else if (userByUsername != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format(baseErrorMessage, "username", "is"));
        } else if (userByEmail != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format(baseErrorMessage, "email", "is"));
        }
    }



    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "This user could not be found"));
    }

    public User createGuestUser() {
        User guestUser = new User();
        UserProfile profile = new UserProfile();
        profile.setUsername("guest_" + UUID.randomUUID().toString().substring(0, 8));
        String password = UUID.randomUUID().toString(); // merken für login!
        profile.setPassword(password);
        profile.setEmail(UUID.randomUUID().toString() + "@guest.com");
        guestUser.setUserProfile(profile);
        guestUser.setIsGuest(true);

        guestUser = registerUser(guestUser);
        return loginUser(profile.getUsername(), password);
    }
}
