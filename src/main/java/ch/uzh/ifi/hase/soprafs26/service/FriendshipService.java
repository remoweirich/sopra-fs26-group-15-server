package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.MessageType;
import ch.uzh.ifi.hase.soprafs26.entity.Friendship;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.FriendshipRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.FriendRequestDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.websocket.Message;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FriendshipService {

    private final UserService userService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final FriendshipRepository friendshipRepository;

    public  FriendshipService(UserService userService, SimpMessagingTemplate simpMessagingTemplate, FriendshipRepository friendshipRepository) {
        this.userService = userService;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.friendshipRepository = friendshipRepository;
    }

    public void sendFriendRequest(long sendingUserId, long receivingUserId) {
        User user1 = sendingUserId < receivingUserId ? userService.getUserById(sendingUserId) : userService.getUserById(receivingUserId);
        User user2 = sendingUserId < receivingUserId ? userService.getUserById(receivingUserId) : userService.getUserById(sendingUserId);

        User sendingUser = userService.getUserById(sendingUserId);
        User receivingUser = userService.getUserById(receivingUserId);

        Optional<Friendship> existing = friendshipRepository.findByFriend1AndFriend2(user1, user2);

        if (existing.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Friendship already exists");
        }

        Friendship friendship = new Friendship();
        friendship.setFriend1(user1);
        friendship.setFriend2(user2);
        friendship.setPendingInvitationReceivedBy(receivingUser);
        friendshipRepository.save(friendship);

        FriendRequestDTO friendRequestDTO = new  FriendRequestDTO();
        friendRequestDTO.setUserId(sendingUserId);
        friendRequestDTO.setUsername(sendingUser.getUserProfile().getUsername());
        friendRequestDTO.setFriendShipId(friendship.getFriendshipId());

        Message message = new Message(MessageType.FRIEND_REQUEST, friendRequestDTO);
        simpMessagingTemplate.convertAndSend("/topic/" + receivingUserId + "/friends", message);
    }

    public void acceptFriendship(long acceptingUserId, long requestingUserId) {
        User user1 = acceptingUserId < requestingUserId ? userService.getUserById(acceptingUserId) : userService.getUserById(requestingUserId);
        User user2 = acceptingUserId < requestingUserId ? userService.getUserById(requestingUserId) : userService.getUserById(acceptingUserId);

        Optional<Friendship> existing = friendshipRepository.findByFriend1AndFriend2(user1, user2);

        if (existing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Friendship already exists");
        }

        Friendship friendship = existing.get();

        User acceptingUser = userService.getUserById(acceptingUserId);
        User requestingUser = userService.getUserById(requestingUserId);

        //Check if User may accept
        if (friendship.getPendingInvitationReceivedBy() == null
                || !friendship.getPendingInvitationReceivedBy().equals(acceptingUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have not received this request");
        }

        friendship.setPendingInvitationReceivedBy(null);
        friendshipRepository.save(friendship);

        FriendRequestDTO friendRequestDTO = new  FriendRequestDTO();
        friendRequestDTO.setUserId(acceptingUserId);
        friendRequestDTO.setUsername(acceptingUser.getUserProfile().getUsername());
        friendRequestDTO.setFriendShipId(friendship.getFriendshipId());

        Message message = new Message(MessageType.FRIEND_ACCEPT, friendRequestDTO);
        simpMessagingTemplate.convertAndSend("/topic/" + requestingUser.getUserId() + "/friends", message);
    }

    public List<User> getFriends(Long userId) {
        User user = userService.getUserById(userId);

        List<User> friends = new ArrayList<>();

        List<Friendship> friendshipList = friendshipRepository.findAllByUser(user);
        for (Friendship friendship : friendshipList) {
            if (friendship.getFriend1().equals(user)) {
                friends.add(friendship.getFriend2());
            } else if (friendship.getFriend2().equals(user)) {
                friends.add(friendship.getFriend1());
            }
        }
        return friends;
    }

    public List<User> getPendingRequestsReceived(Long userId) {
        User user = userService.getUserById(userId);

        List<User> pendingRequests = new ArrayList<>();

        List<Friendship> friendshipList = friendshipRepository.findAllPendingReceivedByUser(user);
        for (Friendship friendship : friendshipList) {
            if (friendship.getFriend1().equals(user)) {
                pendingRequests.add(friendship.getFriend2());
            } else if (friendship.getFriend2().equals(user)) {
                pendingRequests.add(friendship.getFriend1());
            }
        }
        return pendingRequests;
    }

    public List<User> getPendingRequestsSent(Long userId) {
        User user = userService.getUserById(userId);

        List<User> pendingRequests = new ArrayList<>();

        List<Friendship> friendshipList = friendshipRepository.findAllPendingSentByUser(user);
        for (Friendship friendship : friendshipList) {
            if (friendship.getFriend1().equals(user)) {
                pendingRequests.add(friendship.getFriend2());
            }  else if (friendship.getFriend2().equals(user)) {
                pendingRequests.add(friendship.getFriend1());
            }
        }
        return pendingRequests;


    }

}
