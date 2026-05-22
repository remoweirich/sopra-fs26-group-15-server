package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CreateLobbyPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameResultDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyAccessDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyCodePostDTO;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.security.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LobbyRESTController.class)
public class LobbyRESTControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LobbyService lobbyService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private LobbyRepository lobbyRepository;


    // =========================================================
    // POST /lobbies
    // =========================================================

    @Test
    public void createLobby_authenticatedUser_returnsCreated() throws Exception {
        LobbyAccessDTO lobbyAccessDTO = new LobbyAccessDTO();
        lobbyAccessDTO.setLobbyId(1L);
        lobbyAccessDTO.setLobbyCode("ABCD");
        lobbyAccessDTO.setUserId(1L);
        lobbyAccessDTO.setToken("valid-token");

        CreateLobbyPostDTO createLobbyPostDTO = new CreateLobbyPostDTO();
        createLobbyPostDTO.setLobbyName("Test Lobby");
        createLobbyPostDTO.setMaxPlayers(4);
        createLobbyPostDTO.setVisibility(LobbyVisibility.PUBLIC);
        createLobbyPostDTO.setMaxRounds(3);

        given(authService.authUser(Mockito.any())).willReturn(true);
        given(lobbyService.createLobby(Mockito.any(), Mockito.eq(false), Mockito.eq(1L), Mockito.eq("valid-token")))
                .willReturn(lobbyAccessDTO);

        mockMvc.perform(post("/lobbies")
                        .header("token", "valid-token")
                        .header("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(createLobbyPostDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lobbyId", is(1)))
                .andExpect(jsonPath("$.lobbyCode", is("ABCD")));
    }

    @Test
    public void createLobby_invalidToken_returnsUnauthorized() throws Exception {
        CreateLobbyPostDTO createLobbyPostDTO = new CreateLobbyPostDTO();
        createLobbyPostDTO.setLobbyName("Test Lobby");
        createLobbyPostDTO.setMaxPlayers(4);
        createLobbyPostDTO.setVisibility(LobbyVisibility.PUBLIC);
        createLobbyPostDTO.setMaxRounds(3);

        given(authService.authUser(Mockito.any())).willReturn(false);

        mockMvc.perform(post("/lobbies")
                        .header("token", "invalid-token")
                        .header("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(createLobbyPostDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void createLobby_guestUser_returnsCreated() throws Exception {
        // When authService throws NOT_FOUND (userId doesn't exist), the controller
        // falls back to guest-user creation
        LobbyAccessDTO lobbyAccessDTO = new LobbyAccessDTO();
        lobbyAccessDTO.setLobbyId(1L);
        lobbyAccessDTO.setLobbyCode("WXYZ");
        lobbyAccessDTO.setUserId(99L);
        lobbyAccessDTO.setToken("guest-token");

        CreateLobbyPostDTO createLobbyPostDTO = new CreateLobbyPostDTO();
        createLobbyPostDTO.setLobbyName("Guest Lobby");
        createLobbyPostDTO.setMaxPlayers(4);
        createLobbyPostDTO.setVisibility(LobbyVisibility.PUBLIC);
        createLobbyPostDTO.setMaxRounds(3);

        given(authService.authUser(Mockito.any()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        given(lobbyService.createLobby(Mockito.any(), Mockito.eq(true), Mockito.isNull(), Mockito.isNull()))
                .willReturn(lobbyAccessDTO);

        mockMvc.perform(post("/lobbies")
                        .header("token", "guest-token")
                        .header("userId", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(createLobbyPostDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lobbyId", is(1)))
                .andExpect(jsonPath("$.lobbyCode", is("WXYZ")));
    }

    // =========================================================
    // GET /lobbies
    // =========================================================

    @Test
    public void getAllLobbies_returnsLobbies() throws Exception {
        Lobby lobby = buildLobby(1L, "Public Lobby", "ABCD", LobbyVisibility.PUBLIC);

        given(lobbyService.getAllLobbies()).willReturn(List.of(lobby));

        mockMvc.perform(get("/lobbies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].lobbyName", is("Public Lobby")))
                .andExpect(jsonPath("$[0].lobbyCode", is("ABCD")));
    }

    @Test
    public void getAllLobbies_privateLobbiesHaveEmptyCode() throws Exception {
        // Private lobbies must have their code blanked out in the response
        Lobby lobby = buildLobby(2L, "Private Lobby", "SECRET", LobbyVisibility.PRIVATE);

        given(lobbyService.getAllLobbies()).willReturn(List.of(lobby));

        mockMvc.perform(get("/lobbies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].lobbyName", is("Private Lobby")))
                .andExpect(jsonPath("$[0].lobbyCode", is("")));
    }

    @Test
    public void getAllLobbies_emptyList_returnsEmptyArray() throws Exception {
        given(lobbyService.getAllLobbies()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/lobbies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // =========================================================
    // POST /lobbies/{id}
    // =========================================================

    @Test
    public void joinLobby_authenticatedUser_returnsOk() throws Exception {
        LobbyAccessDTO lobbyAccessDTO = new LobbyAccessDTO();
        lobbyAccessDTO.setLobbyId(1L);
        lobbyAccessDTO.setLobbyCode("ABCD");
        lobbyAccessDTO.setUserId(1L);
        lobbyAccessDTO.setToken("valid-token");

        LobbyCodePostDTO lobbyCodeDTO = new LobbyCodePostDTO();
        lobbyCodeDTO.setLobbyCode("ABCD");

        given(authService.authUser(Mockito.any())).willReturn(true);
        given(lobbyService.joinLobby(
                Mockito.eq(1L), Mockito.eq("valid-token"),
                Mockito.eq(1L), Mockito.eq("ABCD"), Mockito.eq(false)))
                .willReturn(lobbyAccessDTO);

        mockMvc.perform(post("/lobbies/1")
                        .header("token", "valid-token")
                        .header("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(lobbyCodeDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lobbyId", is(1)))
                .andExpect(jsonPath("$.lobbyCode", is("ABCD")));
    }

    @Test
    public void joinLobby_guestUser_returnsLobbyAccessDTO() throws Exception {
        // When authService throws NOT_FOUND, the controller joins as guest
        LobbyAccessDTO lobbyAccessDTO = new LobbyAccessDTO();
        lobbyAccessDTO.setLobbyId(1L);
        lobbyAccessDTO.setLobbyCode("ABCD");
        lobbyAccessDTO.setUserId(99L);
        lobbyAccessDTO.setToken("guest-token");

        LobbyCodePostDTO lobbyCodeDTO = new LobbyCodePostDTO();
        lobbyCodeDTO.setLobbyCode("ABCD");

        given(authService.authUser(Mockito.any()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        given(lobbyService.joinLobby(
                Mockito.isNull(), Mockito.isNull(),
                Mockito.eq(1L), Mockito.eq("ABCD"), Mockito.eq(true)))
                .willReturn(lobbyAccessDTO);

        mockMvc.perform(post("/lobbies/1")
                        .header("token", "guest-token")
                        .header("userId", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(lobbyCodeDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lobbyId", is(1)));
    }

    @Test
    public void joinLobby_invalidToken_returnsUnauthorized() throws Exception {
        LobbyCodePostDTO lobbyCodeDTO = new LobbyCodePostDTO();
        lobbyCodeDTO.setLobbyCode("ABCD");

        given(authService.authUser(Mockito.any())).willReturn(false);

        mockMvc.perform(post("/lobbies/1")
                        .header("token", "invalid-token")
                        .header("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(lobbyCodeDTO)))
                .andExpect(status().isUnauthorized());
    }


    // =========================================================
    // GET /lobbies/{lobbyId}
    // =========================================================

    @Test
    public void getMyLobby_authenticated_returnsMyLobbyDTO() throws Exception {
        User admin = new User();
        admin.setUserId(1L);
        admin.setToken("valid-token");

        Lobby lobby = buildLobby(1L, "My Lobby", "ABCD", LobbyVisibility.PUBLIC);
        lobby.setAdmin(admin);
        lobby.setCurrentRound(1);

        given(authService.authUser(Mockito.any())).willReturn(true);
        given(lobbyService.getLobby(1L, 1L)).willReturn(lobby);

        mockMvc.perform(get("/lobbies/1")
                        .header("token", "valid-token")
                        .header("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lobbyId", is(1)))
                .andExpect(jsonPath("$.lobbyName", is("My Lobby")))
                .andExpect(jsonPath("$.lobbyCode", is("ABCD")));
    }

    @Test
    public void getMyLobby_unauthorized_returnsUnauthorized() throws Exception {
        given(authService.authUser(Mockito.any())).willReturn(false);

        mockMvc.perform(get("/lobbies/1")
                        .header("token", "invalid-token")
                        .header("userId", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getMyLobby_userNotInLobby_returnsNotFound() throws Exception {
        given(authService.authUser(Mockito.any())).willReturn(true);
        given(lobbyService.getLobby(Mockito.anyLong(), Mockito.anyLong()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not in this lobby"));

        mockMvc.perform(get("/lobbies/1")
                        .header("token", "valid-token")
                        .header("userId", 1L))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    // GET /game/{gameId}/leaderboard
    // =========================================================

    @Test
    public void leaderboard_authenticated_returnsGameResultDTO() throws Exception {
        GameResultDTO leaderboard = new GameResultDTO();
        leaderboard.setGameId(1L);
        leaderboard.setScores(Collections.emptyList());
        leaderboard.setRounds(Collections.emptyList());
        leaderboard.setUsernames(new HashMap<>());

        given(authService.authUser(Mockito.any())).willReturn(true);
        given(lobbyService.getGameResult(Mockito.anyLong())).willReturn(leaderboard);

        mockMvc.perform(get("/game/1/leaderboard")
                        .header("token", "valid-token")
                        .header("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId", is(1)))
                .andExpect(jsonPath("$.rounds", is(leaderboard.getRounds())))
                .andExpect(jsonPath("$.scores", is(leaderboard.getScores())))
                .andExpect(jsonPath("$.usernames", is(leaderboard.getUsernames())));
    }

    @Test
    public void leaderboard_unauthorized_returnsUnauthorized() throws Exception {
        given(authService.authUser(Mockito.any())).willReturn(false);

        mockMvc.perform(get("/game/1/leaderboard")
                        .header("token", "invalid-token")
                        .header("userId", 1L))
                .andExpect(status().isUnauthorized());
    }

// =========================================================
// POST /lobbies/join/{lobbyCode}
// =========================================================

    @Test
    public void joinLobbyByCode_authenticatedUser_returnsOk() throws Exception {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);

        LobbyAccessDTO lobbyAccessDTO = new LobbyAccessDTO();
        lobbyAccessDTO.setLobbyId(1L);
        lobbyAccessDTO.setLobbyCode("ABCD");
        lobbyAccessDTO.setUserId(1L);
        lobbyAccessDTO.setToken("valid-token");

        LobbyCodePostDTO lobbyCodeDTO = new LobbyCodePostDTO();
        lobbyCodeDTO.setLobbyCode("ABCD");

        given(lobbyRepository.findByLobbyCode("ABCD")).willReturn(java.util.Optional.of(lobby));
        given(authService.authUser(Mockito.any())).willReturn(true);
        given(lobbyService.joinLobby(
                Mockito.eq(1L), Mockito.eq("valid-token"),
                Mockito.eq(1L), Mockito.eq("ABCD"), Mockito.eq(false)))
                .willReturn(lobbyAccessDTO);

        mockMvc.perform(post("/lobbies/join/ABCD")
                        .header("token", "valid-token")
                        .header("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(lobbyCodeDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lobbyId", is(1)))
                .andExpect(jsonPath("$.lobbyCode", is("ABCD")));
    }

    @Test
    public void joinLobbyByCode_invalidToken_returnsUnauthorized() throws Exception {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);

        LobbyCodePostDTO lobbyCodeDTO = new LobbyCodePostDTO();
        lobbyCodeDTO.setLobbyCode("ABCD");

        given(lobbyRepository.findByLobbyCode("ABCD")).willReturn(java.util.Optional.of(lobby));
        given(authService.authUser(Mockito.any())).willReturn(false);

        mockMvc.perform(post("/lobbies/join/ABCD")
                        .header("token", "invalid-token")
                        .header("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(lobbyCodeDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void joinLobbyByCode_guestUser_returnsOk() throws Exception {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);

        LobbyAccessDTO lobbyAccessDTO = new LobbyAccessDTO();
        lobbyAccessDTO.setLobbyId(1L);
        lobbyAccessDTO.setLobbyCode("ABCD");
        lobbyAccessDTO.setUserId(99L);
        lobbyAccessDTO.setToken("guest-token");

        LobbyCodePostDTO lobbyCodeDTO = new LobbyCodePostDTO();
        lobbyCodeDTO.setLobbyCode("ABCD");

        given(lobbyRepository.findByLobbyCode("ABCD")).willReturn(java.util.Optional.of(lobby));
        given(authService.authUser(Mockito.any()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        given(lobbyService.joinLobby(
                Mockito.isNull(), Mockito.isNull(),
                Mockito.eq(1L), Mockito.eq("ABCD"), Mockito.eq(true)))
                .willReturn(lobbyAccessDTO);

        mockMvc.perform(post("/lobbies/join/ABCD")
                        .header("token", "guest-token")
                        .header("userId", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(lobbyCodeDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lobbyId", is(1)))
                .andExpect(jsonPath("$.lobbyCode", is("ABCD")));
    }

    @Test
    public void joinLobbyByCode_lobbyNotFound_returnsNotFound() throws Exception {
        LobbyCodePostDTO lobbyCodeDTO = new LobbyCodePostDTO();
        lobbyCodeDTO.setLobbyCode("NOPE");

        given(lobbyRepository.findByLobbyCode("NOPE")).willReturn(java.util.Optional.empty());

        mockMvc.perform(post("/lobbies/join/NOPE")
                        .header("token", "valid-token")
                        .header("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(lobbyCodeDTO)))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    // Helpers
    // =========================================================

    /**
     * Creates a basic Lobby with all required maps initialized to avoid NPEs
     * when DTOMapper iterates over users/scores collections.
     */
    private Lobby buildLobby(Long id, String name, String code, LobbyVisibility visibility) {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(id);
        lobby.setLobbyName(name);
        lobby.setLobbyCode(code);
        lobby.setVisibility(visibility);
        lobby.setMaxPlayers(4);
        lobby.setMaxRounds(3);
        lobby.setLobbyState(LobbyState.WAITING);
        lobby.setPlayers(new ArrayList<>());
        return lobby;
    }

    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e.toString()));
        }
    }
}
