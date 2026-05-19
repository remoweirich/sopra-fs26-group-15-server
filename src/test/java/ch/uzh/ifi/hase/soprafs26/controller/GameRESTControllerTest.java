package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.MessageType;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ResyncDTO;
import ch.uzh.ifi.hase.soprafs26.security.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GameRESTController.class)
public class GameRESTControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LobbyService lobbyService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private GameService gameService;

    // =========================================================
    // GET /game/{gameId}/resync
    // =========================================================

    @Test
    public void resync_validGame_returnsResyncDTO() throws Exception {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1L);

        ResyncDTO resyncDTO = new ResyncDTO();
        resyncDTO.setType(MessageType.ROUND_START);
        resyncDTO.setRemainingTime(15000L);
        resyncDTO.setMaxRounds(5);
        resyncDTO.setPayload(null);

        given(lobbyService.getLobbyById(1L)).willReturn(lobby);
        given(gameService.resync(lobby)).willReturn(resyncDTO);

        mockMvc.perform(get("/game/1/resync")
                        .header("token", "valid-token")
                        .header("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is(MessageType.ROUND_START.toString())))
                .andExpect(jsonPath("$.remainingTime", is(15000)))
                .andExpect(jsonPath("$.maxRounds", is(5)));
    }

    @Test
    public void resync_gameNotFound_returnsNotFound() throws Exception {
        given(lobbyService.getLobbyById(Mockito.anyLong()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

        mockMvc.perform(get("/game/999/resync")
                        .header("token", "valid-token")
                        .header("userId", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    public void resync_noAuthHeaders_stillReturnsOk() throws Exception {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(2L);

        ResyncDTO resyncDTO = new ResyncDTO();
        resyncDTO.setType(MessageType.SCORES);
        resyncDTO.setRemainingTime(5000L);
        resyncDTO.setMaxRounds(3);

        given(lobbyService.getLobbyById(2L)).willReturn(lobby);
        given(gameService.resync(lobby)).willReturn(resyncDTO);

        mockMvc.perform(get("/game/2/resync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxRounds", is(3)));
    }
}
