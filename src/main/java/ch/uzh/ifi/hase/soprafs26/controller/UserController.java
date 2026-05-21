package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LoginPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RegisterPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UpdateUserPutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserAuthDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.security.AuthHeader;
import ch.uzh.ifi.hase.soprafs26.security.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.AchievementService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;


@RestController
public class UserController {

	private final UserService userService;
	private final AuthService authService;
    private final AchievementService achievementService;

	UserController(UserService userService, AuthService authService, AchievementService achievementService) {
		this.userService = userService;
		this.authService = authService;
        this.achievementService = achievementService;
    }

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
			MyUserDTO myUserDTO = DTOMapper.INSTANCE.convertUserToMyUserDTO(user);
            myUserDTO.setUserAchievementDTOList(userService.getUserAchievements(user));
            return myUserDTO;
		} else {
			UserDTO userDTO = DTOMapper.INSTANCE.convertUserToUserDTO(user);
            userDTO.setUserAchievementDTOList(userService.getUserAchievements(user));
            return userDTO;
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

    @DeleteMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@RequestHeader("token") String token, @PathVariable("userId") Long userId) {
        AuthHeader authHeader = new AuthHeader(userId, token);
        if (!authService.authUser(authHeader)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        userService.deleteUser(userId);
    }

    @GetMapping("/users/search")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<UserDTO> searchUsers(@RequestParam("username") String username) {
        List<UserDTO> sanitizedUsers = new ArrayList<>();

        List<User> users = userService.searchUsers(username);
        for  (User user : users) {
            sanitizedUsers.add(DTOMapper.INSTANCE.convertUserToUserDTO(user));
        }
        return sanitizedUsers;
    }

    @PostMapping("/award/kingbababui")
    @ResponseStatus(HttpStatus.CREATED)
    public void awardKingBabaBui(
            @RequestHeader("userId") Long sendingUserId,
            @RequestHeader("token") String token) {
        AuthHeader authHeader = new AuthHeader(sendingUserId, token);
        if (!token.isEmpty() && authService.authUser(authHeader)) {
            achievementService.KingBabaBui(userService.getUserById(sendingUserId));
        }
    }
}

