package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;
import ch.uzh.ifi.hase.soprafs26.entity.GameResult;
import ch.uzh.ifi.hase.soprafs26.objects.Admin;
import ch.uzh.ifi.hase.soprafs26.objects.Lobby;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.CreateLobbyPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyAccessDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyCodePostDTO;
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
    private GameRepository gameRepository;

    // =========================================================
    // POST /lobbies
    // =========================================================

    @Test
    public void createLobby_authenticatedUser_returnsCreated() throws Exception {
        LobbyAccessDTO accessDTO = new LobbyAccessDTO(1L, "ABCD");
        accessDTO.setUserId(1L);
        accessDTO.setToken("valid-token");

        CreateLobbyPostDTO createDTO = new CreateLobbyPostDTO();
        createDTO.setLobbyName("Test Lobby");
        createDTO.setSize(4);
        createDTO.setVisibility(LobbyVisibility.PUBLIC);
        createDTO.setMaxRounds(3);

        given(authService.authUser(Mockito.any())).willReturn(true);
        given(lobbyService.createLobby(Mockito.any(), Mockito.eq(false), Mockito.eq(1L), Mockito.eq("valid-token")))
                .willReturn(accessDTO);

        mockMvc.perform(post("/lobbies")
                        .header("token", "valid-token")
                        .header("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lobbyId", is(1)))
                .andExpect(jsonPath("$.lobbyCode", is("ABCD")));
    }

    @Test
    public void createLobby_invalidToken_returnsUnauthorized() throws Exception {
        CreateLobbyPostDTO createDTO = new CreateLobbyPostDTO();
        createDTO.setLobbyName("Test Lobby");
        createDTO.setSize(4);
        createDTO.setVisibility(LobbyVisibility.PUBLIC);
        createDTO.setMaxRounds(3);

        given(authService.authUser(Mockito.any())).willReturn(false);

        mockMvc.perform(post("/lobbies")
                        .header("token", "invalid-token")
                        .header("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(createDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void createLobby_guestUser_createsGuestAndLobby() throws Exception {
        // When authService throws NOT_FOUND (userId doesn't exist), the controller
        // falls back to guest-user creation
        LobbyAccessDTO accessDTO = new LobbyAccessDTO(2L, "WXYZ");
        accessDTO.setUserId(99L);
        accessDTO.setToken("guest-token");

        CreateLobbyPostDTO createDTO = new CreateLobbyPostDTO();
        createDTO.setLobbyName("Guest Lobby");
        createDTO.setSize(4);
        createDTO.setVisibility(LobbyVisibility.PUBLIC);
        createDTO.setMaxRounds(3);

        given(authService.authUser(Mockito.any()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        given(lobbyService.createLobby(Mockito.any(), Mockito.eq(true), Mockito.isNull(), Mockito.isNull()))
                .willReturn(accessDTO);

        mockMvc.perform(post("/lobbies")
                        .header("token", "guest-token")
                        .header("userId", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lobbyId", is(2)))
                .andExpect(jsonPath("$.lobbyCode", is("WXYZ")));
    }

    // =========================================================
    // GET /lobbies
    // =========================================================

    @Test
    public void getAllLobbies_returnsPublicLobbies() throws Exception {
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
        LobbyAccessDTO accessDTO = new LobbyAccessDTO(1L, "ABCD");
        accessDTO.setUserId(1L);
        accessDTO.setToken("valid-token");

        LobbyCodePostDTO lobbyCodeDTO = new LobbyCodePostDTO();
        lobbyCodeDTO.setLobbyCode("ABCD");

        given(authService.authUser(Mockito.any())).willReturn(true);
        given(lobbyService.joinLobby(
                Mockito.eq(1L), Mockito.eq("valid-token"),
                Mockito.eq(1L), Mockito.eq("ABCD"), Mockito.eq(false)))
                .willReturn(accessDTO);

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
    public void joinLobby_guestUser_createsGuestAndJoins() throws Exception {
        // When authService throws NOT_FOUND, the controller joins as guest
        LobbyAccessDTO accessDTO = new LobbyAccessDTO(1L, "ABCD");
        accessDTO.setUserId(99L);
        accessDTO.setToken("guest-token");

        LobbyCodePostDTO lobbyCodeDTO = new LobbyCodePostDTO();
        lobbyCodeDTO.setLobbyCode("ABCD");

        given(authService.authUser(Mockito.any()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        given(lobbyService.joinLobby(
                Mockito.isNull(), Mockito.isNull(),
                Mockito.eq(1L), Mockito.eq("ABCD"), Mockito.eq(true)))
                .willReturn(accessDTO);

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
        Lobby lobby = buildLobby(1L, "My Lobby", "ABCD", LobbyVisibility.PUBLIC);
        lobby.setAdmin(new Admin(1L, "valid-token"));
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
        GameResult gameResult = new GameResult();
        gameResult.setGameId(1L);
        gameResult.setScores(Collections.emptyList());
        gameResult.setRounds(Collections.emptyList());
        gameResult.setUsernames(new HashMap<>());

        given(authService.authUser(Mockito.any())).willReturn(true);
        given(gameRepository.findByGameId(1L)).willReturn(gameResult);

        mockMvc.perform(get("/game/1/leaderboard")
                        .header("token", "valid-token")
                        .header("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId", is(1)));
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
        lobby.setSize(4);
        lobby.setMaxRounds(3);
        lobby.setLobbyState(LobbyState.WAITING);
        lobby.setUsers(new HashMap<>());
        lobby.setScores(new HashMap<>());
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
