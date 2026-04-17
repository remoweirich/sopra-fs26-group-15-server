package ch.uzh.ifi.hase.soprafs26.websocket;

import ch.uzh.ifi.hase.soprafs26.security.AuthHeader;
import ch.uzh.ifi.hase.soprafs26.security.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class TopicSubscriptionInterceptor implements ChannelInterceptor {

    private final AuthService authService;

    @Autowired
    public TopicSubscriptionInterceptor(@Lazy AuthService authService) {
        this.authService = authService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        StompCommand command = accessor.getCommand();

        // Use a switch to handle different STOMP lifecycle events
        switch (command) {
            case SUBSCRIBE -> handleSubscription(accessor);
            case SEND -> handlePublish(accessor);
            case CONNECT -> handleConnect(accessor);
            // You can add CONNECT here later for global authentication
            default -> { /* Allow other commands like DISCONNECT/HEARTBEAT */ }
        }

        return message;
    }

    private void handleSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        String userId = accessor.getFirstNativeHeader("userId");
        String token = accessor.getFirstNativeHeader("token");

        if (destination != null && (destination.startsWith("/topic/lobby/")) || destination.startsWith("/topic/game/")) {
            String[] parts = destination.split("/");
            String lobbyId = parts[3];
            if (!canAccessLobby(userId, token, lobbyId)) {
                throw new IllegalArgumentException("Cannot subscribe: Permission denied for lobby " + lobbyId);
            }
        }
    }

    private void handlePublish(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        String userId = accessor.getFirstNativeHeader("userId");
        String token = accessor.getFirstNativeHeader("token");


        // Example: Only allow users to publish to a lobby if they are in it
        if (destination != null && (destination.startsWith("/app/lobby/")) || destination.startsWith("/app/game/")) {
            String[] parts = destination.split("/");
            // Index 0: "" (empty before the first slash)
            // Index 1: "app"
            // Index 2: "lobby" | "game"
            // Index 3: "{lobbyId}"
            String lobbyId = parts[3];
            if (!canAccessLobby(userId, token, lobbyId)) {
                throw new IllegalArgumentException("Cannot publish: You are not a member of lobby " + lobbyId);
            }
        }
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        Long userId = Long.parseLong(accessor.getFirstNativeHeader("userId"));
        String token = accessor.getFirstNativeHeader("token");

        if (!authService.authUser(new AuthHeader(userId, token))){
            throw new IllegalArgumentException("Cannot connect: Invalid credentials");
        }
    }


    private boolean canAccessLobby(String userId, String token, String lobbyId) {
        // TODO: Call authService to check if the user is actually a member of this lobby in the database
        long lobbyById = Long.parseLong(lobbyId);
        long userById = Long.parseLong(userId);

        return authService.isUserInLobby(userById, token, lobbyById);
    }
}