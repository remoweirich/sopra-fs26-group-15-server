package ch.uzh.ifi.hase.soprafs26.service;

import ch.guessbb.sopraserver.constant.*;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;
import ch.guessbb.sopraserver.entity.*;
import ch.uzh.ifi.hase.soprafs26.constant.MessageType;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.RoundHistory;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.RoundHistoryRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.guessbb.sopraserver.rest.dto.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.websocket.Message;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class LobbyService {

    private final UserService userService;
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final LobbyRepository lobbyRepository;
    private final RoundHistoryRepository roundHistoryRepository;

    private final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXY1Z23456789";
    private final SecureRandom RANDOM = new SecureRandom();

    public LobbyService(UserService userService, GameService gameService, SimpMessagingTemplate messagingTemplate, UserRepository userRepository, LobbyRepository lobbyRepository, RoundHistoryRepository roundHistoryRepository)  {
        this.userService = userService;
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
        this.lobbyRepository = lobbyRepository;
        this.roundHistoryRepository = roundHistoryRepository;
    }

    public LobbyAccessDTO createLobby(CreateLobbyPostDTO createLobbyPostDTO, boolean isGuest, Long userId, String token) {
        if (isGuest) {
            User guestUser = userService.createGuestUser();
            userId = guestUser.getUserId();
            token = guestUser.getToken();
        }

        User admin = userService.getUserById(userId);

        Lobby newLobby = new Lobby();
        newLobby.setLobbyName(createLobbyPostDTO.getLobbyName());
        newLobby.setLobbyCode(createLobbyCode());
        newLobby.setAdmin(admin);
        newLobby.setMaxPlayers(createLobbyPostDTO.getMaxPlayers());
        newLobby.setVisibility(createLobbyPostDTO.getVisibility());
        newLobby.setMaxRounds(createLobbyPostDTO.getMaxRounds());
        newLobby.setLobbyState(LobbyState.WAITING);
        newLobby.setPlayers(new ArrayList<>());

        newLobby = lobbyRepository.save(newLobby);
        lobbyRepository.flush();

        LobbyAccessDTO dto = new LobbyAccessDTO();
        dto.setLobbyId(newLobby.getLobbyId());
        dto.setLobbyCode(newLobby.getLobbyCode());
        dto.setUserId(userId);
        dto.setToken(token);

        return dto;
    }

    public List<Lobby> getAllLobbies() {
        return lobbyRepository.findAll().stream()
                .filter(l -> l.getLobbyState() != LobbyState.FINISHED)
                .collect(Collectors.toList());
    }

    public LobbyAccessDTO joinLobby(Long userId, String token, Long lobbyId, String lobbyCode, Boolean isGuest) {
        Long effectiveUserId = userId;
        String effectiveToken = token;

        if (isGuest) {
            User guestUser = userService.createGuestUser();
            effectiveUserId = guestUser.getUserId();
            effectiveToken = guestUser.getToken();
        }

        final Long finalUserId = effectiveUserId;
        final String finalToken = effectiveToken;

        Lobby lobby = getLobbyById(lobbyId);
        User user = userService.getUserById(finalUserId);

        // Check whether the lobby code is correct
        if (!lobby.getLobbyCode().equals(lobbyCode)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Incorrect lobby code");
        }

        // Check if user is already in lobby
        boolean alreadyInLobby = lobby.getPlayers().stream()
                .anyMatch(p -> p.getUserId().equals(finalUserId));
        if (alreadyInLobby) {
            LobbyAccessDTO dto = new LobbyAccessDTO();
            dto.setLobbyId(lobbyId);
            dto.setLobbyCode(lobbyCode);
            dto.setUserId(finalUserId);
            dto.setToken(finalToken);
            return dto;
        }

        // Check whether the lobby is full
        if (lobby.getPlayers().size() >= lobby.getMaxPlayers()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lobby is full");
        }

        // Add user to lobby
        lobby.getPlayers().add(user);
        lobbyRepository.save(lobby);
        lobbyRepository.flush();

        // Send broadcast message
        MyLobbyDTO myLobbyDTO = DTOMapper.INSTANCE.convertEntityToMyLobbyDTO(lobby);
        myLobbyDTO.setCurrentPlayers(lobby.getPlayers().size());
        Message message = new Message(MessageType.LOBBY_STATE, myLobbyDTO);
        messagingTemplate.convertAndSend("/topic/lobby/" + lobby.getLobbyId(), message);

        LobbyAccessDTO dto = new LobbyAccessDTO();
        dto.setLobbyId(lobbyId);
        dto.setLobbyCode(lobbyCode);
        dto.setUserId(finalUserId);
        dto.setToken(finalToken);
        return dto;
    }

    public Lobby getLobby(Long lobbyId, Long userId) {
        Lobby lobby = getLobbyById(lobbyId);
        boolean isInLobby = lobby.getPlayers().stream()
                .anyMatch(p -> p.getUserId().equals(userId));
        if (!isInLobby) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must join the lobby first!");
        }
        return lobby;
    }

    public Lobby getLobbyById(Long lobbyId) {
        return lobbyRepository.findById(lobbyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));
    }

    private Lobby getLobbyByCode(String lobbyCode) {
        return lobbyRepository.findByLobbyCode(lobbyCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));
    }


    public String createLobbyCode() {
        StringBuilder sb = new StringBuilder(4);
        do {
            sb.setLength(0);
            for (int i = 0; i < 4; i++) {
                sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
            }
        } while (existsByCode(sb.toString()));
        return sb.toString();
    }

    private boolean existsByCode(String code) {
        return lobbyRepository.existsByLobbyCode(code);
    }


    public void startGame(Long lobbyId) {
        Lobby lobby = getLobbyById(lobbyId);

        if (lobby.getLobbyState().equals(LobbyState.IN_GAME)) {
            return; // Already started, skip
        }

        // Runden erstellen via GameService
        gameService.setupGame(lobby);

        // Lobby State updaten
        lobby.setLobbyState(LobbyState.IN_GAME);
        lobbyRepository.save(lobby);
        lobbyRepository.flush();

        // Alle Clients benachrichtigen
        Message startMessage = new Message(MessageType.GAME_START, null);
        messagingTemplate.convertAndSend("/topic/lobby/" + lobbyId, startMessage);
    }

    public void leaveLobby(Long lobbyId, Long userId) {
        Lobby lobby = getLobbyById(lobbyId);

        // Remove user from lobby
        lobby.getPlayers().removeIf(p -> p.getUserId().equals(userId));

        // If user was admin, assign new admin
        if (lobby.getAdmin().getUserId().equals(userId)) {
            if (!lobby.getPlayers().isEmpty()) {
                lobby.setAdmin(lobby.getPlayers().get(0));
            } else {
                // No players left → delete lobby
                lobbyRepository.delete(lobby);
                return;
            }
        }

        lobbyRepository.save(lobby);
        lobbyRepository.flush();

        // Send broadcast message
        MyLobbyDTO myLobbyDTO = DTOMapper.INSTANCE.convertEntityToMyLobbyDTO(lobby);
        myLobbyDTO.setCurrentPlayers(lobby.getPlayers().size());
        Message message = new Message(MessageType.LOBBY_STATE, myLobbyDTO);
        messagingTemplate.convertAndSend("/topic/lobby/" + lobby.getLobbyId(), message);
    }

    public GameResultDTO getGameResult(Long gameId) {
        List<RoundHistory> roundHistories = roundHistoryRepository.findByLobbyLobbyId(gameId);

        Map<Integer, RoundResultDTO> roundMap = new LinkedHashMap<>();
        Map<Long, Integer> totalScores = new HashMap<>();
        Map<Long, String> usernames = new HashMap<>();

        for (RoundHistory rh : roundHistories) {
            Long uid = rh.getUser().getUserId();
            String username = rh.getUser().getUserProfile().getUsername();
            usernames.put(uid, username);

            totalScores.merge(uid, rh.getPoints(), Integer::sum);

            RoundResultDTO roundDTO = roundMap.computeIfAbsent(rh.getRoundNumber(), n -> {
                RoundResultDTO r = new RoundResultDTO();
                r.setRoundNumber(n);
                r.setScores(new HashMap<>());
                r.setDistances(new HashMap<>());
                return r;
            });
            roundDTO.getScores().put(uid, rh.getPoints());
            roundDTO.getDistances().put(uid, (double) rh.getDistanceToTrain());
        }

        List<ScoreDTO> scores = totalScores.entrySet().stream()
                .map(e -> {
                    ScoreDTO s = new ScoreDTO();
                    s.setUserId(e.getKey());
                    s.setPoints(e.getValue());
                    return s;
                })
                .sorted((a, b) -> b.getPoints() - a.getPoints())
                .collect(Collectors.toList());

        GameResultDTO result = new GameResultDTO();
        result.setGameId(gameId);
        result.setRounds(new ArrayList<>(roundMap.values()));
        result.setScores(scores);
        result.setUsernames(usernames);

        return result;
    }
}