package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Friendship;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs26.repository.FriendshipRepository;
import ch.uzh.ifi.hase.soprafs26.websocket.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FriendshipServiceTest {

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private UserService userService;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @InjectMocks
    private FriendshipService friendshipService;

    private User user1;
    private User user2;
    private Friendship friendship;

    @BeforeEach
    public void setup() {
        // Setup User 1
        user1 = new User();
        user1.setUserId(1L);
        UserProfile profile1 = new UserProfile();
        profile1.setUsername("user1");
        user1.setUserProfile(profile1);

        // Setup User 2
        user2 = new User();
        user2.setUserId(2L);
        UserProfile profile2 = new UserProfile();
        profile2.setUsername("user2");
        user2.setUserProfile(profile2);

        // Setup Friendship
        friendship = new Friendship();
        friendship.setFriendshipId(100L);
        friendship.setFriend1(user1);
        friendship.setFriend2(user2);
    }

    // =======================================================================
    // Tests for sendFriendRequest
    // =======================================================================

    @Test
    public void sendFriendRequest_validInputs_success() {
        when(userService.getUserById(1L)).thenReturn(user1);
        when(userService.getUserById(2L)).thenReturn(user2);
        when(friendshipRepository.findByFriend1AndFriend2(user1, user2)).thenReturn(Optional.empty());

        friendshipService.sendFriendRequest(1L, 2L);

        // Verify save was called
        verify(friendshipRepository, times(1)).save(any(Friendship.class));

        // Verify websocket messages were sent
        verify(simpMessagingTemplate, times(1)).convertAndSend(eq("/topic/2/notifications"), any(Message.class));
        verify(simpMessagingTemplate, times(1)).convertAndSend(eq("/topic/1/notifications"), any(Message.class));
    }

    @Test
    public void sendFriendRequest_friendshipExists_throwsConflict() {
        when(userService.getUserById(1L)).thenReturn(user1);
        when(userService.getUserById(2L)).thenReturn(user2);
        when(friendshipRepository.findByFriend1AndFriend2(user1, user2)).thenReturn(Optional.of(friendship));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> friendshipService.sendFriendRequest(1L, 2L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Friendship already exists", exception.getReason());
    }

    // =======================================================================
    // Tests for acceptFriendship
    // =======================================================================

    @Test
    public void acceptFriendship_validInputs_success() {
        // user2 (id=2) accepts request from user1 (id=1)
        friendship.setPendingInvitationReceivedBy(user2);

        when(userService.getUserById(1L)).thenReturn(user1);
        when(userService.getUserById(2L)).thenReturn(user2);
        when(friendshipRepository.findByFriend1AndFriend2(user1, user2)).thenReturn(Optional.of(friendship));

        friendshipService.acceptFriendship(2L, 1L);

        assertNull(friendship.getPendingInvitationReceivedBy());
        verify(friendshipRepository, times(1)).save(friendship);
        verify(simpMessagingTemplate, times(1)).convertAndSend(eq("/topic/1/notifications"), any(Message.class));
    }

    @Test
    public void acceptFriendship_notReceivedByUser_throwsForbidden() {
        // user2 tries to accept, but user1 is the one who received it
        friendship.setPendingInvitationReceivedBy(user1);

        when(userService.getUserById(1L)).thenReturn(user1);
        when(userService.getUserById(2L)).thenReturn(user2);
        when(friendshipRepository.findByFriend1AndFriend2(user1, user2)).thenReturn(Optional.of(friendship));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> friendshipService.acceptFriendship(2L, 1L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("You have not received this request", exception.getReason());
    }

    // =======================================================================
    // Tests for getFriends & Requests
    // =======================================================================

    @Test
    public void getFriends_returnsFriendList() {
        List<Friendship> friendships = new ArrayList<>();
        friendships.add(friendship);

        when(userService.getUserById(1L)).thenReturn(user1);
        when(friendshipRepository.findAllByUser(user1)).thenReturn(friendships);

        List<User> friends = friendshipService.getFriends(1L);

        assertEquals(1, friends.size());
        assertEquals(user2, friends.get(0));
    }

    @Test
    public void getPendingRequestsReceived_returnsList() {
        friendship.setPendingInvitationReceivedBy(user1);
        List<Friendship> friendships = new ArrayList<>();
        friendships.add(friendship);

        when(userService.getUserById(1L)).thenReturn(user1);
        when(friendshipRepository.findAllPendingReceivedByUser(user1)).thenReturn(friendships);

        List<User> pending = friendshipService.getPendingRequestsReceived(1L);

        assertEquals(1, pending.size());
        assertEquals(user2, pending.get(0));
    }

    // =======================================================================
    // Tests for removeFriendship
    // =======================================================================

    @Test
    public void removeFriendship_validInputs_success() {
        when(userService.getUserById(1L)).thenReturn(user1);
        when(userService.getUserById(2L)).thenReturn(user2);
        when(friendshipRepository.findByFriend1AndFriend2(user1, user2)).thenReturn(Optional.of(friendship));

        friendshipService.removeFriendship(1L, 2L);

        verify(friendshipRepository, times(1)).delete(friendship);
    }

    @Test
    public void removeFriendship_notFound_throwsException() {
        when(userService.getUserById(1L)).thenReturn(user1);
        when(userService.getUserById(2L)).thenReturn(user2);
        when(friendshipRepository.findByFriend1AndFriend2(user1, user2)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> friendshipService.removeFriendship(1L, 2L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    // =======================================================================
    // Tests for rejectFriendRequest
    // =======================================================================

    @Test
    public void rejectFriendRequest_validInputs_success() {
        friendship.setPendingInvitationReceivedBy(user2);

        when(userService.getUserById(1L)).thenReturn(user1);
        when(userService.getUserById(2L)).thenReturn(user2);
        when(friendshipRepository.findByFriend1AndFriend2(user1, user2)).thenReturn(Optional.of(friendship));

        friendshipService.rejectFriendRequest(2L, 1L);

        verify(friendshipRepository, times(1)).deleteByFriendshipId(friendship.getFriendshipId());
        verify(simpMessagingTemplate, times(1)).convertAndSend(eq("/topic/1/notifications"), any(Message.class));
    }



    @Test
    public void getPendingRequestsSent_userIsFriend1_returnsList() {
        friendship.setPendingInvitationReceivedBy(user2);
        List<Friendship> friendships = new ArrayList<>();
        friendships.add(friendship);

        when(userService.getUserById(1L)).thenReturn(user1);
        when(friendshipRepository.findAllPendingSentByUser(user1)).thenReturn(friendships);

        List<User> pending = friendshipService.getPendingRequestsSent(1L);

        assertEquals(1, pending.size());
        assertEquals(user2, pending.get(0));
    }

    @Test
    public void getPendingRequestsSent_userIsFriend2_returnsList() {
        // user2 hat die Anfrage gesendet, ist aber friend2 in der Friendship
        Friendship flipped = new Friendship();
        flipped.setFriendshipId(101L);
        flipped.setFriend1(user1);
        flipped.setFriend2(user2);
        flipped.setPendingInvitationReceivedBy(user1);

        List<Friendship> friendships = new ArrayList<>();
        friendships.add(flipped);

        when(userService.getUserById(2L)).thenReturn(user2);
        when(friendshipRepository.findAllPendingSentByUser(user2)).thenReturn(friendships);

        List<User> pending = friendshipService.getPendingRequestsSent(2L);

        assertEquals(1, pending.size());
        assertEquals(user1, pending.get(0));
    }



    @Test
    public void acceptFriendship_friendshipNotFound_throwsConflict() {
        when(userService.getUserById(1L)).thenReturn(user1);
        when(userService.getUserById(2L)).thenReturn(user2);
        when(friendshipRepository.findByFriend1AndFriend2(user1, user2)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> friendshipService.acceptFriendship(2L, 1L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Friendship doesn't exists", exception.getReason());
    }



    @Test
    public void rejectFriendRequest_notReceivedByUser_throwsForbidden() {
        // user1 hat die Anfrage empfangen, aber user2 versucht abzulehnen
        friendship.setPendingInvitationReceivedBy(user1);

        when(userService.getUserById(1L)).thenReturn(user1);
        when(userService.getUserById(2L)).thenReturn(user2);
        when(friendshipRepository.findByFriend1AndFriend2(user1, user2)).thenReturn(Optional.of(friendship));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> friendshipService.rejectFriendRequest(2L, 1L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("You have not received this request", exception.getReason());
    }

    @Test
    public void rejectFriendRequest_friendshipNotFound_throwsConflict() {
        when(userService.getUserById(1L)).thenReturn(user1);
        when(userService.getUserById(2L)).thenReturn(user2);
        when(friendshipRepository.findByFriend1AndFriend2(user1, user2)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> friendshipService.rejectFriendRequest(2L, 1L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }



    @Test
    public void getFriends_userIsFriend2_returnsFriend1() {
        // user2 fragt seine Freunde ab — er ist friend2 in der Friendship
        List<Friendship> friendships = new ArrayList<>();
        friendships.add(friendship); // friend1=user1, friend2=user2

        when(userService.getUserById(2L)).thenReturn(user2);
        when(friendshipRepository.findAllByUser(user2)).thenReturn(friendships);

        List<User> friends = friendshipService.getFriends(2L);

        assertEquals(1, friends.size());
        assertEquals(user1, friends.get(0));
    }

    @Test
    public void getPendingRequestsReceived_userIsFriend2_returnsFriend1() {
        friendship.setPendingInvitationReceivedBy(user2);
        List<Friendship> friendships = new ArrayList<>();
        friendships.add(friendship);

        when(userService.getUserById(2L)).thenReturn(user2);
        when(friendshipRepository.findAllPendingReceivedByUser(user2)).thenReturn(friendships);

        List<User> pending = friendshipService.getPendingRequestsReceived(2L);

        assertEquals(1, pending.size());
        assertEquals(user1, pending.get(0));
    }
}