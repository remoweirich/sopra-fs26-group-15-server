package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.security.AuthHeader;
import ch.uzh.ifi.hase.soprafs26.security.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.FriendshipService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RestController
public class FriendshipController {

    private final UserService userService;
    private final FriendshipService friendshipService;
    private final AuthService authService;

    public FriendshipController(UserService userService,  FriendshipService friendshipService,  AuthService authService) {
        this.userService = userService;
        this.friendshipService = friendshipService;
        this.authService = authService;
    }

    @PostMapping("/friends/request/{receivingUserId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void sendFriendRequest(
            @PathVariable("receivingUserId") Long receivingUserId,
            @RequestHeader("userId") Long sendingUserId,
            @RequestHeader("token") String token) {

        AuthHeader authHeader = new AuthHeader(sendingUserId, token);
        boolean isvalid = authService.authUser(authHeader);

        if (!isvalid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong credentials.");
        }

        if (userService.getUserById(sendingUserId).getIsGuest()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot add friends as a guest user.");
        }

        if (userService.getUserById(receivingUserId).getIsGuest()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot add a guest user as a friend.");
        }

        friendshipService.sendFriendRequest(sendingUserId, receivingUserId);
    }

    @PostMapping("/friends/accept/{requestingUserId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void acceptFriendship(
            @PathVariable("requestingUserId") Long requestingUserId,
            @RequestHeader("userId") Long acceptingUserId,
            @RequestHeader("token") String token) {

        AuthHeader authHeader = new AuthHeader(acceptingUserId, token);
        boolean isvalid = authService.authUser(authHeader);

        if (!isvalid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong credentials.");
        }

        friendshipService.acceptFriendship(acceptingUserId, requestingUserId);

    }

    @PostMapping("/friends/reject/{requestingUserId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void rejectFriendRequest(
            @PathVariable("requestingUserId") Long requestingUserId,
            @RequestHeader("userId") Long rejectingUserId,
            @RequestHeader("token") String token) {

        AuthHeader authHeader = new AuthHeader(rejectingUserId, token);
        boolean isvalid = authService.authUser(authHeader);

        if (!isvalid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong credentials.");
        }

        friendshipService.rejectFriendRequest(rejectingUserId, requestingUserId);

    }


    @GetMapping("/friends/{userId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public List<UserDTO> getFriends(@PathVariable("userId") Long userId,
                                    @RequestHeader("token") String token) {

        AuthHeader authHeader = new AuthHeader(userId, token);
        authService.authUser(authHeader);

        List<UserDTO> sanitizedFriends = new ArrayList<>();

        List<User> friends = friendshipService.getFriends(userId);
        for (User friend : friends) {
            sanitizedFriends.add(DTOMapper.INSTANCE.convertUserToUserDTO(friend));
        }

        return  sanitizedFriends;
    }

    @GetMapping("friends/{userId}/pendingReceived")
    @ResponseStatus(HttpStatus.OK)
    public List<UserDTO> getPendingRequestsReceived(@PathVariable("userId") Long userId,
                                                    @RequestHeader("token") String token) {

        AuthHeader authHeader = new AuthHeader(userId, token);
        boolean isvalid = authService.authUser(authHeader);

        if (!isvalid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong credentials.");
        }
        List<UserDTO> sanitizedUsers = new ArrayList<>();

        List<User> pendingRequestsReceived = friendshipService.getPendingRequestsReceived(userId);
        for (User user : pendingRequestsReceived) {
            sanitizedUsers.add(DTOMapper.INSTANCE.convertUserToUserDTO(user));
        }

        return  sanitizedUsers;
    }

    @GetMapping("friends/{userId}/pendingSent")
    @ResponseStatus(HttpStatus.OK)
    public List<UserDTO> getPendingRequestsSent(@PathVariable("userId") Long userId,
                                                @RequestHeader("token") String token) {

        AuthHeader authHeader = new AuthHeader(userId, token);
        boolean isvalid = authService.authUser(authHeader);

        if (!isvalid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong credentials.");
        }
        List<UserDTO> sanitizedUsers = new ArrayList<>();
        for (User user : friendshipService.getPendingRequestsSent(userId)) {
            sanitizedUsers.add(DTOMapper.INSTANCE.convertUserToUserDTO(user));
        }

        return  sanitizedUsers;
    }
}
