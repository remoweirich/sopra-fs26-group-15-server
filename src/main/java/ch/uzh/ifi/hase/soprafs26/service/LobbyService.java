package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;
import ch.uzh.ifi.hase.soprafs26.objects.Admin;
import ch.uzh.ifi.hase.soprafs26.objects.Game;
import ch.uzh.ifi.hase.soprafs26.objects.Lobby;
import ch.uzh.ifi.hase.soprafs26.objects.Score;
import ch.uzh.ifi.hase.soprafs26.objects.Round;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CreateLobbyPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyAccessDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;

import java.security.SecureRandom;

import ch.uzh.ifi.hase.soprafs26.entity.*;

import ch.uzh.ifi.hase.soprafs26.security.AuthService;

import ch.uzh.ifi.hase.soprafs26.rest.dto.MyLobbyDTO;

import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;

import ch.uzh.ifi.hase.soprafs26.constant.*;
import ch.uzh.ifi.hase.soprafs26.websocket.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;

@Service
@Transactional
public class LobbyService {

    private List<Lobby> activeLobbies = new ArrayList<>();
    //private final AuthService authService;
    private final UserService userService;
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;
    private long newLobbyId = 1L;


    public LobbyService(/*AuthService authService,*/ UserService userService, GameService gameService, SimpMessagingTemplate messagingTemplate, UserRepository userRepository) {
        //this.authService = authService;
        this.userService = userService;
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    public List<Lobby> getAllLobbies() {
        return activeLobbies;
    }

    private final UserRepository userRepository;

    private final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXY1Z23456789";
    private final SecureRandom RANDOM = new SecureRandom();

    public LobbyAccessDTO createLobby(CreateLobbyPostDTO createLobbyPostDTO, boolean isGuest, Long userId, String token) {
        if (isGuest) {
            User guestUser = createGuestUser();

            userId = guestUser.getUserId();
            token = guestUser.getToken();
        }

        Lobby newLobby = new Lobby();

        newLobby.setLobbyId(newLobbyId++);

        newLobby.setLobbyName(createLobbyPostDTO.getLobbyName());

        String newLobbyCode = createLobbyCode(); // to be replaced by random code
        newLobby.setLobbyCode(newLobbyCode);

        Admin newAdmin = new Admin(userId, token);
        newLobby.setAdmin(newAdmin);

        newLobby.setSize(createLobbyPostDTO.getSize());

        newLobby.setVisibility(createLobbyPostDTO.getVisibility());

//        Map<Long, User> users = new HashMap<>();
//        User currentUser = userRepository.findById(createLobbyPostDTO.getUserId()).orElse(null);
//        users.put(createLobbyPostDTO.getUserId(), currentUser);
//        newLobby.setUsers(users);

        newLobby.setCurrentRound(0);

        newLobby.setMaxRounds(createLobbyPostDTO.getMaxRounds());

        Map<Long, Score> scores = new HashMap<>();
        newLobby.setScores(scores);

        newLobby.setUsers(new HashMap<>());

        newLobby.setLobbyState(LobbyState.WAITING);

        Game newGame = new Game();
        newLobby.setGame(newGame);

        activeLobbies.add(newLobby);

        LobbyAccessDTO dto = DTOMapper.INSTANCE.convertLobbyToLobbyAccessDTO(newLobby);
        dto.setUserId(userId);
        dto.setToken(token);

        return dto;
    }

    public LobbyAccessDTO joinLobby(Long userId,String token, Long lobbyId, String lobbyCode, Boolean isGuest) {
        LobbyAccessDTO lobbyAccessDTO = new LobbyAccessDTO(lobbyId, lobbyCode);


        if (isGuest) {
            User guestUser = createGuestUser();
            userId = guestUser.getUserId();
            token = guestUser.getToken();
        }

        Lobby lobby = getLobbyById(lobbyId);


        User user = userService.getUserById(userId);

        //Check whether the lobby code is correct
        if (!lobby.getLobbyCode().equals(lobbyCode)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Incorrect lobby code");
        }

        //Check if user is already in lobby
        if(lobby.existsUser(userId)) {
            lobbyAccessDTO.setLobbyCode(lobbyCode);
            lobbyAccessDTO.setLobbyId(lobbyId);
            lobbyAccessDTO.setUserId(userId);
            lobbyAccessDTO.setToken(token);

            return lobbyAccessDTO;
        }


        // Check whether the lobby is full
        if (lobby.getUsers().size() >= lobby.getSize()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lobby is full");
        }
        // add user to lobby
        lobby.addUser(userId, user);

        //if Lobby is now full: if game is public, start game, else wait for admin to start the game
        if (lobby.getUsers().size() >= lobby.getSize() && lobby.getVisibility() == LobbyVisibility.PUBLIC && !lobby.getLobbyState().equals(LobbyState.IN_GAME)) {
            //lobby.setLobbyState(LobbyState.IN_GAME);
            startGame(lobby.getLobbyId());
        }

        //send broadcast message to lobby that user has joined
        MyLobbyDTO myLobbyDTO = DTOMapper.INSTANCE.convertEntityToMyLobbyDTO(lobby);
        Message message = new Message(MessageType.LOBBY_STATE, myLobbyDTO);
        messagingTemplate.convertAndSend("/topic/lobby/" + lobby.getLobbyId(), message);

        lobbyAccessDTO.setUserId(userId);
        lobbyAccessDTO.setToken(token);

        return lobbyAccessDTO;
    }

    public void startGame(Long lobbyId) {

        Lobby lobby = getLobbyById(lobbyId);
/* 
        if (lobby.getLobbyState().equals(LobbyState.IN_GAME)) {
        return;  // Already started, skip
    }*/

        //create a Game object and fetch the Train data
        Game game = gameService.setupGame(lobby);

        //update the Lobby object
        lobby.setGame(game);
        lobby.setLobbyState(LobbyState.IN_GAME);

        Message startMessage = new Message(MessageType.GAME_START, null);
        messagingTemplate.convertAndSend("/topic/lobby/" + lobbyId, startMessage);
    }

    public void leaveLobby(Long lobbyId, Long userId) {
        System.out.println("In lobbyService");
        Lobby lobby = getLobbyById(lobbyId);
        //change from user based to userId since I (Shadi) changed users to be a map indexed by userId
        //remove user from lobby
        lobby.removeUser(userId);

        //if user was admin, assign new admin
        if (lobby.getAdmin().getUserId().equals(userId)) {
            if (!lobby.getUsers().isEmpty()) {
                User newAdminUser = lobby.getUsers().get(0);
                Admin newAdmin = new Admin(newAdminUser.getUserId(), newAdminUser.getToken());
                lobby.setAdmin(newAdmin);
            } else {
                //if no users are left in the lobby, delete the lobby
                activeLobbies.remove(lobby);
                return;
            }
        }

        //send broadcast message to lobby that user has left
        MyLobbyDTO myLobbyDTO = DTOMapper.INSTANCE.convertEntityToMyLobbyDTO(lobby);
        Message message = new Message(MessageType.LOBBY_STATE, myLobbyDTO);
        messagingTemplate.convertAndSend("/topic/lobby/" + lobby.getLobbyId(), message);
        System.out.println("Message sent from lobbyService");
    }

    public Lobby getLobby(Long lobbyId, Long userId) {
        Lobby lobby = getLobbyById(lobbyId);
        if (!lobby.existsUser(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must join the lobby first!");
        } else {
            return lobby;
        }
    }

    public Lobby getLobbyById(Long lobbyId) {
        for (Lobby lobby : activeLobbies) {
            if (lobby.getLobbyId().equals(lobbyId)) {
                return lobby;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found");
    }

    private Lobby getLobbyByCode(String lobbyCode) {
        for (Lobby lobby : activeLobbies) {
            if (lobby.getLobbyCode().equals(lobbyCode)) {
                return lobby;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found");
    }


    public String createLobbyCode() {
        StringBuilder sb = new StringBuilder(4);
        do {
            sb.setLength(0);
            for (int i = 0; i < 4; i++) {
                sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
            }
        }
        while (existsByCode(sb.toString()));


        return sb.toString();
    }


    public boolean existsByCode(String code) {
        return activeLobbies.stream()
                .anyMatch(lobby -> lobby.getLobbyCode().equals(code));
    }

    public User createGuestUser() {
        User guestUser = new User();
        guestUser.setUsername("guest_" + UUID.randomUUID().toString().substring(0, 8));
        guestUser.setPassword(UUID.randomUUID().toString()); // dummy password
        guestUser.setEmail(UUID.randomUUID().toString() + "@guest.com"); // dummy email
        guestUser.setIsGuest(true);

        guestUser = userService.registerUser(guestUser);
        User loggedInGuest = userService.loginUser(guestUser.getUsername(), guestUser.getPassword());
        return loggedInGuest;
    }
}