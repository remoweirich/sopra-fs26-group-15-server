package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs26.security.AuthHeader;
import ch.uzh.ifi.hase.soprafs26.security.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.FriendshipService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FriendshipController.class)
public class FriendshipControllerTest {

    private static final String VALID_TOKEN = "valid-token";
    private static final String INVALID_TOKEN = "invalid-token";
    private static final Long SENDER_ID = 1L;
    private static final Long RECEIVER_ID = 2L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private FriendshipService friendshipService;

    @MockitoBean
    private AuthService authService;

    private User sender;
    private User receiver;

    @BeforeEach
    public void setup() {
        sender = createUser(SENDER_ID, false);
        receiver = createUser(RECEIVER_ID, false);
    }

    // --- sendFriendRequest ---

    @Test
    public void sendFriendRequest_success() throws Exception {
        given(authService.authUser(any(AuthHeader.class))).willReturn(true);
        given(userService.getUserById(SENDER_ID)).willReturn(sender);
        given(userService.getUserById(RECEIVER_ID)).willReturn(receiver);
        doNothing().when(friendshipService).sendFriendRequest(SENDER_ID, RECEIVER_ID);

        performPost("/friends/request/" + RECEIVER_ID, SENDER_ID, VALID_TOKEN)
                .andExpect(status().isCreated());

        verify(friendshipService, Mockito.times(1)).sendFriendRequest(SENDER_ID, RECEIVER_ID);
    }

    @Test
    public void sendFriendRequest_invalidToken_throwsUnauthorized() throws Exception {
        given(authService.authUser(any(AuthHeader.class))).willReturn(false);

        performPost("/friends/request/" + RECEIVER_ID, SENDER_ID, INVALID_TOKEN)
                .andExpect(status().isUnauthorized());

        verify(friendshipService, Mockito.never()).sendFriendRequest(SENDER_ID, RECEIVER_ID);
    }

    @Test
    public void sendFriendRequest_senderIsGuest_throwsForbidden() throws Exception {
        sender.setIsGuest(true);
        given(authService.authUser(any(AuthHeader.class))).willReturn(true);
        given(userService.getUserById(SENDER_ID)).willReturn(sender);

        performPost("/friends/request/" + RECEIVER_ID, SENDER_ID, VALID_TOKEN)
                .andExpect(status().isForbidden());
    }

    @Test
    public void sendFriendRequest_receiverIsGuest_throwsForbidden() throws Exception {
        receiver.setIsGuest(true);
        given(authService.authUser(any(AuthHeader.class))).willReturn(true);
        given(userService.getUserById(SENDER_ID)).willReturn(sender);
        given(userService.getUserById(RECEIVER_ID)).willReturn(receiver);

        performPost("/friends/request/" + RECEIVER_ID, SENDER_ID, VALID_TOKEN)
                .andExpect(status().isForbidden());
    }

    // --- acceptFriendship ---

    @Test
    public void acceptFriendship_success() throws Exception {
        Long requestingUserId = 3L;
        given(authService.authUser(any(AuthHeader.class))).willReturn(true);
        doNothing().when(friendshipService).acceptFriendship(SENDER_ID, requestingUserId);

        performPost("/friends/accept/" + requestingUserId, SENDER_ID, VALID_TOKEN)
                .andExpect(status().isOk());

        verify(friendshipService, Mockito.times(1)).acceptFriendship(SENDER_ID, requestingUserId);
    }

    @Test
    public void acceptFriendship_invalidToken_throwsUnauthorized() throws Exception {
        Long requestingUserId = 3L;
        given(authService.authUser(any(AuthHeader.class))).willReturn(false);

        performPost("/friends/accept/" + requestingUserId, SENDER_ID, INVALID_TOKEN)
                .andExpect(status().isUnauthorized());

        verify(friendshipService, Mockito.never()).acceptFriendship(anyLong(), anyLong());
    }

    // --- rejectFriendRequest ---

    @Test
    public void rejectFriendRequest_success() throws Exception {
        Long requestingUserId = 4L;
        given(authService.authUser(any(AuthHeader.class))).willReturn(true);
        doNothing().when(friendshipService).rejectFriendRequest(SENDER_ID, requestingUserId);

        performPost("/friends/reject/" + requestingUserId, SENDER_ID, VALID_TOKEN)
                .andExpect(status().isOk());

        verify(friendshipService, Mockito.times(1)).rejectFriendRequest(SENDER_ID, requestingUserId);
    }

    @Test
    public void rejectFriendRequest_invalidToken_throwsUnauthorized() throws Exception {
        Long requestingUserId = 4L;
        given(authService.authUser(any(AuthHeader.class))).willReturn(false);

        performPost("/friends/reject/" + requestingUserId, SENDER_ID, INVALID_TOKEN)
                .andExpect(status().isUnauthorized());

        verify(friendshipService, Mockito.never()).rejectFriendRequest(anyLong(), anyLong());
    }

    // --- getFriends ---

    @Test
    public void getFriends_success_returnsFriendsList() throws Exception {
        User friend1 = createUser(10L, false);
        friend1.getUserProfile().setUsername("marvin");

        User friend2 = createUser(11L, false);
        friend2.getUserProfile().setUsername("arthur");

        given(authService.authUser(any(AuthHeader.class))).willReturn(true);
        given(friendshipService.getFriends(SENDER_ID)).willReturn(List.of(friend1, friend2));

        performGet("/friends/" + SENDER_ID, VALID_TOKEN)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].userId").value(10))
                .andExpect(jsonPath("$[0].username").value("marvin"))
                .andExpect(jsonPath("$[1].userId").value(11))
                .andExpect(jsonPath("$[1].username").value("arthur"));
    }

    @Test
    public void getFriends_invalidToken_throwsUnauthorized() throws Exception {
        Mockito.doThrow(new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong credentials."))
                .when(authService).authUser(any(AuthHeader.class));

        performGet("/friends/" + SENDER_ID, INVALID_TOKEN)
                .andExpect(status().isUnauthorized());

        verify(friendshipService, Mockito.never()).getFriends(anyLong());
    }

    // --- getPendingRequestsReceived ---

    @Test
    public void getPendingRequestsReceived_success_returnsPendingList() throws Exception {
        User requester1 = createUser(20L, false);
        requester1.getUserProfile().setUsername("ford");

        User requester2 = createUser(21L, false);
        requester2.getUserProfile().setUsername("zaphod");

        given(authService.authUser(any(AuthHeader.class))).willReturn(true);
        given(friendshipService.getPendingRequestsReceived(SENDER_ID)).willReturn(List.of(requester1, requester2));

        performGet("/friends/" + SENDER_ID + "/pendingReceived", VALID_TOKEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].userId").value(20))
                .andExpect(jsonPath("$[0].username").value("ford"))
                .andExpect(jsonPath("$[1].userId").value(21))
                .andExpect(jsonPath("$[1].username").value("zaphod"));

        verify(friendshipService, Mockito.times(1)).getPendingRequestsReceived(SENDER_ID);
    }

    @Test
    public void getPendingRequestsReceived_invalidToken_throwsUnauthorized() throws Exception {
        given(authService.authUser(any(AuthHeader.class))).willReturn(false);

        performGet("/friends/" + SENDER_ID + "/pendingReceived", INVALID_TOKEN)
                .andExpect(status().isUnauthorized());

        verify(friendshipService, Mockito.never()).getPendingRequestsReceived(anyLong());
    }

    // --- getPendingRequestsSent ---

    @Test
    public void getPendingRequestsSent_success_returnsPendingList() throws Exception {
        User recipient1 = createUser(30L, false);
        recipient1.getUserProfile().setUsername("trillian");

        given(authService.authUser(any(AuthHeader.class))).willReturn(true);
        given(friendshipService.getPendingRequestsSent(SENDER_ID)).willReturn(List.of(recipient1));

        performGet("/friends/" + SENDER_ID + "/pendingSent", VALID_TOKEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(30))
                .andExpect(jsonPath("$[0].username").value("trillian"));

        verify(friendshipService, Mockito.times(1)).getPendingRequestsSent(SENDER_ID);
    }

    @Test
    public void getPendingRequestsSent_invalidToken_throwsUnauthorized() throws Exception {
        given(authService.authUser(any(AuthHeader.class))).willReturn(false);

        performGet("/friends/" + SENDER_ID + "/pendingSent", INVALID_TOKEN)
                .andExpect(status().isUnauthorized());

        verify(friendshipService, Mockito.never()).getPendingRequestsSent(anyLong());
    }

    // --- removeFriendship ---

    @Test
    public void removeFriendship_success() throws Exception {
        Long userIdToDelete = 5L;
        given(authService.authUser(any(AuthHeader.class))).willReturn(true);
        doNothing().when(friendshipService).removeFriendship(SENDER_ID, userIdToDelete);

        performDelete("/friends/remove/" + userIdToDelete, SENDER_ID, VALID_TOKEN)
                .andExpect(status().isOk());

        verify(friendshipService, Mockito.times(1)).removeFriendship(SENDER_ID, userIdToDelete);
    }

    @Test
    public void removeFriendship_invalidToken_throwsUnauthorized() throws Exception {
        Long userIdToDelete = 5L;
        given(authService.authUser(any(AuthHeader.class))).willReturn(false);

        performDelete("/friends/remove/" + userIdToDelete, SENDER_ID, INVALID_TOKEN)
                .andExpect(status().isUnauthorized());

        verify(friendshipService, Mockito.never()).removeFriendship(anyLong(), anyLong());
    }

    // --- Helper Methods ---

    private User createUser(Long id, boolean isGuest) {
        User user = new User();
        user.setUserId(id);
        user.setIsGuest(isGuest);
        user.setUserProfile(new UserProfile());
        return user;
    }

    private ResultActions performPost(String url, Long userId, String token) throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(url)
                .header("userId", userId.toString())
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON);
        return mockMvc.perform(request);
    }

    private ResultActions performGet(String url, String token) throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(url)
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON);
        return mockMvc.perform(request);
    }

    private ResultActions performDelete(String url, Long userId, String token) throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.delete(url)
                .header("userId", userId.toString())
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON);
        return mockMvc.perform(request);
    }
}