package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import ch.uzh.ifi.hase.soprafs26.security.AuthService;
import ch.uzh.ifi.hase.soprafs26.security.AuthHeader;
import org.springframework.web.server.ResponseStatusException;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;

/**
 * HELÖOOOOOOOOOOOO
 */

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */

@RestController
public class UserController {

	private final UserService userService;
	private final AuthService authService;

	UserController(UserService userService, AuthService authService) {
		this.userService = userService;
		this.authService = authService;
	}

	// ENPOINTS

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public UserAuthDTO registerUser(@RequestBody RegisterPostDTO registerPostDTO) {

		User userInput = DTOMapper.INSTANCE.convertRegisterPostDTOtoUser(registerPostDTO);

		User registeredUser = userService.registerUser(userInput);

		return DTOMapper.INSTANCE.convertUsertoUserAuthDTO(registeredUser);

	}

	@PostMapping("/login")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserAuthDTO loginUser(@RequestBody LoginPostDTO loginPostDTO) {

		User user = userService.loginUser(
				loginPostDTO.getUsername(),
				loginPostDTO.getPassword());
		return DTOMapper.INSTANCE.convertUsertoUserAuthDTO(user);
	}

	@GetMapping("/users/{userId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Object getUser(
            @PathVariable("userId") Long userId,
			@RequestHeader(value = "token", required = false, defaultValue = "") String token) {


		AuthHeader authHeader = new AuthHeader(userId, token);
		boolean isAuthenticated = authService.authUser(authHeader);
		User user = userService.getUserById(userId);

		if (isAuthenticated) {
			return DTOMapper.INSTANCE.convertUserToMyUserDTO(user);
		} else {
			return DTOMapper.INSTANCE.convertUserToUserDTO(user);
		}
	}

	@PostMapping("/users/{userId}/logout")
	@ResponseStatus(HttpStatus.OK)
	public void logoutUser(@RequestHeader("token") String token, @PathVariable("userId") Long userId) {

		AuthHeader authHeader = new AuthHeader(userId, token);
		if (!authService.authUser(authHeader)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
		}

		userService.logoutUser(authHeader);
	}

    @PutMapping("users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateUser(@RequestHeader("token") String token, @PathVariable("userId") Long userId, @RequestBody UpdateUserPutDTO updateUserPutDTO){
        AuthHeader authHeader = new AuthHeader(userId, token);
        if (!authService.authUser(authHeader)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        userService.updateUser(userId, updateUserPutDTO);
    }
}

// @RestController
// public class UserController {

// private final UserService userService;

// UserController(UserService userService) {
// this.userService = userService;
// }

// @GetMapping("/users")
// @ResponseStatus(HttpStatus.OK)
// @ResponseBody
// public List<UserGetDTO> getAllUsers() {
// // fetch all users in the internal representation
// List<User> users = userService.getUsers();
// List<UserGetDTO> userGetDTOs = new ArrayList<>();

// // convert each user to the API representation
// for (User user : users) {
// userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
// }
// return userGetDTOs;
// }

// @PostMapping("/users")
// @ResponseStatus(HttpStatus.CREATED)
// @ResponseBody
// public UserGetDTO createUser(@RequestBody UserPostDTO userPostDTO) {
// // convert API user to internal representation
// User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

// // create user
// User createdUser = userService.createUser(userInput);
// // convert internal representation of user back to API
// return DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
// }
// }
