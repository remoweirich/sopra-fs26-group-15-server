    package ch.uzh.ifi.hase.soprafs26.controller;

    import ch.uzh.ifi.hase.soprafs26.constant.*;
    import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
    import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
    import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
    import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
    import ch.uzh.ifi.hase.soprafs26.security.AuthHeader;
    import ch.uzh.ifi.hase.soprafs26.security.AuthService;
    import ch.uzh.ifi.hase.soprafs26.service.GameService;
    import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
    import org.springframework.http.HttpStatus;
    import org.springframework.web.bind.annotation.*;
    import org.springframework.web.server.ResponseStatusException;



    @RestController
    public class GameRESTController {

        private final LobbyService lobbyService;
        private final AuthService authService;
        private final GameService gameService;

        public GameRESTController(LobbyService lobbyService, AuthService authService, GameService gameService) {
            this.lobbyService = lobbyService;
            this.authService = authService;
            this.gameService = gameService;
        }

        @GetMapping("/game/{gameId}/resync")
            @ResponseStatus(HttpStatus.OK)
            @ResponseBody
            public ResyncDTO resync(
                    @RequestHeader(value = "token", required = false) String token,
                    @RequestHeader(value = "userId", required = false) Long userId,
                    @PathVariable("gameId") Long gameId
        ) {
                        Lobby currentLobby = lobbyService.getLobbyById(gameId);

                        return gameService.resync(currentLobby);
                    }

    }

