package ch.uzh.ifi.hase.soprafs26.websocket;

import ch.uzh.ifi.hase.soprafs26.security.AuthHeader;
import ch.uzh.ifi.hase.soprafs26.security.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class WebSocketAuthInterceptorTest {

    @Mock
    private AuthService authService;

    @Mock
    private MessageChannel channel;

    @InjectMocks
    private WebSocketAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ═══════════════════════════════════════════════════════════════════
    // CONNECT
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: Eine CONNECT-Nachricht mit gueltigen Credentials wird durchgelassen.
     * Faengt Bug: Wenn handleConnect() immer wirft, koennten sich keine Clients verbinden.
     */
    @Test
    void preSend_connect_validCredentials_returnsMessage() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("userId", "1");
        accessor.setNativeHeader("token", "valid-token");
        var message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(authService.authUser(any(AuthHeader.class))).thenReturn(true);

        var result = interceptor.preSend(message, channel);
        assertNotNull(result);
    }

    /**
     * Prueft: Eine CONNECT-Nachricht mit ungueltigen Credentials wirft
     * eine IllegalArgumentException.
     * Faengt Bug: Ohne diesen Wurf koennte jeder Client eine WebSocket-Session
     * oeffnen, auch ohne gueltigen Token.
     */
    @Test
    void preSend_connect_invalidCredentials_throwsException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("userId", "1");
        accessor.setNativeHeader("token", "invalid-token");
        var message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(authService.authUser(any(AuthHeader.class))).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> interceptor.preSend(message, channel));
    }

    // ═══════════════════════════════════════════════════════════════════
    // SUBSCRIBE
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: Ein SUBSCRIBE auf /topic/lobby/{id} wird zugelassen.
     */
    @Test
    void preSend_subscribe_lobbyTopic_allowed() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/lobby/100");
        accessor.setNativeHeader("userId", "1");
        accessor.setNativeHeader("token", "valid-token");
        var message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        var result = interceptor.preSend(message, channel);

        assertNotNull(result);
    }

    // ═══════════════════════════════════════════════════════════════════
    // SEND
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: Ein SEND auf /app/game/{id} wird zugelassen.
     */
    @Test
    void preSend_publish_gameDestination_allowed() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/game/200");
        accessor.setNativeHeader("userId", "2");
        accessor.setNativeHeader("token", "valid-token");
        var message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        var result = interceptor.preSend(message, channel);

        assertNotNull(result);
    }

    // ═══════════════════════════════════════════════════════════════════
    // Other commands
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: DISCONNECT und andere Befehle werden ohne Validierung durchgelassen.
     * Faengt Bug: Wuerde der default-Zweig fehlen, wuerden DISCONNECT/HEARTBEAT
     * einen NullPointerException ausloesen (kein Destination-Header gesetzt).
     */
    @Test
    void preSend_disconnect_passesThrough() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        var message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        var result = interceptor.preSend(message, channel);

        assertEquals(message, result);
    }
}
