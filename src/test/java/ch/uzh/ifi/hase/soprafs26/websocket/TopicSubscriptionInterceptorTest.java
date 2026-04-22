package ch.uzh.ifi.hase.soprafs26.websocket;

import ch.uzh.ifi.hase.soprafs26.security.AuthHeader;
import ch.uzh.ifi.hase.soprafs26.security.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class TopicSubscriptionInterceptorTest {

    @Mock
    private AuthService authService;

    @Mock
    private MessageChannel channel;

    @InjectMocks
    private TopicSubscriptionInterceptor interceptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void preSend_Connect_Success() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("userId", "1");
        accessor.setNativeHeader("token", "valid-token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(authService.authUser(any(AuthHeader.class))).thenReturn(true);

        Message<?> result = interceptor.preSend(message, channel);
        assertNotNull(result);
    }

    @Test
    void preSend_Connect_Failure_ThrowsException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("userId", "1");
        accessor.setNativeHeader("token", "invalid-token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(authService.authUser(any(AuthHeader.class))).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> interceptor.preSend(message, channel));
    }

    @Test
    void preSend_Subscribe_Lobby_Success() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/lobby/100");
        accessor.setNativeHeader("userId", "1");
        accessor.setNativeHeader("token", "valid-token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(authService.isUserInLobby(1L, "valid-token", 100L)).thenReturn(true);

        Message<?> result = interceptor.preSend(message, channel);
        assertNotNull(result);
    }

    @Test
    void preSend_Subscribe_Lobby_Denied_ThrowsException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/lobby/100");
        accessor.setNativeHeader("userId", "1");
        accessor.setNativeHeader("token", "valid-token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(authService.isUserInLobby(1L, "valid-token", 100L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> interceptor.preSend(message, channel));
    }

    @Test
    void preSend_Publish_Game_Success() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/game/200");
        accessor.setNativeHeader("userId", "2");
        accessor.setNativeHeader("token", "valid-token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(authService.isUserInLobby(2L, "valid-token", 200L)).thenReturn(true);

        Message<?> result = interceptor.preSend(message, channel);
        assertNotNull(result);
    }

    @Test
    void preSend_OtherCommand_Allowed() {
        // Test that DISCONNECT or other commands pass through without validation
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);
        assertEquals(message, result);
    }
}