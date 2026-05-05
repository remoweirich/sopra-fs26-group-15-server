package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.guessbb.sopraserver.rest.dto.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LoginPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RegisterPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UpdateUserPutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserAuthDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.security.AuthHeader;
import ch.uzh.ifi.hase.soprafs26.security.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


@RestController
public class UserController {

	private final UserService userService;
	private final AuthService authService;

	UserController(UserService userService, AuthService authService) {
		this.userService = userService;
		this.authService = authService;
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

