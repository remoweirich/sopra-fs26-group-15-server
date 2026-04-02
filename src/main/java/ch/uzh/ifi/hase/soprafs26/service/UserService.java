package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.rest.dto.UpdateUserPutDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.UserScoreboard;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.security.AuthHeader;

import java.util.List;
import java.util.UUID;
import java.util.Date;

/**
 * HELLLLÖOOOOOOOOO
 */

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

	private final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public List<User> getUsers() {
		return this.userRepository.findAll();
	}

	public User registerUser(User newUser) {

		UserScoreboard userScoreboard = new UserScoreboard();
		userScoreboard.setTotalPoints(0);
		userScoreboard.setGamesPlayed(0);
		userScoreboard.setGamesWon(0);
		userScoreboard.setGuessingPrecision(0f);

		newUser.setUserScoreboard(userScoreboard);

		// String newUserToken;

		// do {
		// newUserToken = UUID.randomUUID().toString();
		// } while (userRepository.findByToken(newUserToken) != null);

		// newUser.setToken(newUserToken);

		newUser.setStatus(UserStatus.OFFLINE);

		newUser.setCreationDate(new Date());

		checkIfUserExists(newUser);

		newUser = userRepository.save(newUser);
		userRepository.flush();

		log.debug("Created Information for User: {}", newUser);
		return newUser;
	}

	public User loginUser(String username, String password) {
		User loggedInUser = userRepository.findByUsername(username);

		if (loggedInUser == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This user could not be found");
		}
		if (!loggedInUser.getPassword().equals(password)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The credentials are wrong");
		}

		// token erhalten und auf online setzen
		String newToken;

		do {
			newToken = UUID.randomUUID().toString();
		} while (userRepository.findByToken(newToken) != null);

		loggedInUser.setToken(newToken);
		loggedInUser.setStatus(UserStatus.ONLINE);

		loggedInUser = userRepository.save(loggedInUser);
		userRepository.flush();

		return loggedInUser;
	}

	public User getUserById(Long userId) {

		User user = userRepository.findById(userId).orElse(null);
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This user could not be found");
		}
		return user;
	}

	public void logoutUser(AuthHeader authHeader) {
		User user = userRepository.findById(authHeader.getUserId()).orElse(null);
		user.setToken(null);
		user.setStatus(UserStatus.OFFLINE);
		userRepository.save(user);
		userRepository.flush();
	}

	/**
	 * This is a helper method that will check the uniqueness criteria of the
	 * username and the name
	 * defined in the User entity. The method will do nothing if the input is unique
	 * and throw an error otherwise.
	 *
	 * @param userToBeCreated
	 * @throws org.springframework.web.server.ResponseStatusException
	 * @see User
	 */
	private void checkIfUserExists(User userToBeCreated) {
		User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
		User userByEmail = userRepository.findByEmail(userToBeCreated.getEmail());

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

    public void updateUser(Long userId, UpdateUserPutDTO userUpdate) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (userUpdate.getUsername() != null) user.setUsername(userUpdate.getUsername());
        if (userUpdate.getPassword() != null) user.setPassword(userUpdate.getPassword());
        if (userUpdate.getEmail() != null) user.setEmail(userUpdate.getEmail());
        if (userUpdate.getUserBio() != null) user.setUserBio(userUpdate.getUserBio());

        userRepository.save(user);
    }
}
