package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.UserScoreboard;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LoginPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RegisterPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UpdateUserPutDTO;
import ch.uzh.ifi.hase.soprafs26.security.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
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

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    // =========================================================
    // POST /register
    // =========================================================

    @Test
    public void registerUser_validInput_returnsCreated() throws Exception {
        User user = new User();
        user.setUserId(1L);
        user.setUsername("testUser");
        user.setToken("test-token");

        RegisterPostDTO registerPostDTO = new RegisterPostDTO();
        registerPostDTO.setUsername("testUser");
        registerPostDTO.setEmail("test@test.com");
        registerPostDTO.setPassword("password");

        given(userService.registerUser(Mockito.any())).willReturn(user);

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(registerPostDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.token", is("test-token")));
    }

    @Test
    public void registerUser_duplicateUsername_returnsBadRequest() throws Exception {
        RegisterPostDTO registerPostDTO = new RegisterPostDTO();
        registerPostDTO.setUsername("existingUser");
        registerPostDTO.setEmail("test@test.com");
        registerPostDTO.setPassword("password");

        given(userService.registerUser(Mockito.any()))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already taken"));

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(registerPostDTO)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================
    // POST /login
    // =========================================================

    @Test
    public void loginUser_validCredentials_returnsOk() throws Exception {
        User user = new User();
        user.setUserId(2L);
        user.setToken("login-token");

        LoginPostDTO loginPostDTO = new LoginPostDTO();
        loginPostDTO.setUsername("testUser");
        loginPostDTO.setPassword("password");

        given(userService.loginUser("testUser", "password")).willReturn(user);

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(loginPostDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(2)))
                .andExpect(jsonPath("$.token", is("login-token")));
    }

    @Test
    public void loginUser_wrongPassword_returnsUnauthorized() throws Exception {
        LoginPostDTO loginPostDTO = new LoginPostDTO();
        loginPostDTO.setUsername("testUser");
        loginPostDTO.setPassword("wrongpassword");

        given(userService.loginUser(Mockito.any(), Mockito.any()))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The credentials are wrong"));

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(loginPostDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void loginUser_userNotFound_returnsNotFound() throws Exception {
        LoginPostDTO loginPostDTO = new LoginPostDTO();
        loginPostDTO.setUsername("unknownUser");
        loginPostDTO.setPassword("password");

        given(userService.loginUser(Mockito.any(), Mockito.any()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(loginPostDTO)))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    // GET /users/{userId}
    // =========================================================

    @Test
    public void getUser_authenticated_returnsMyUserDTO() throws Exception {
        UserScoreboard scoreboard = new UserScoreboard();
        scoreboard.setTotalPoints(10);
        scoreboard.setGamesPlayed(5);
        scoreboard.setGamesWon(2);
        scoreboard.setGuessingPrecision(0.4f);

        User user = new User();
        user.setUserId(1L);
        user.setUsername("testUser");
        user.setEmail("test@test.com");
        user.setUserBio("Test bio");
        user.setUserScoreboard(scoreboard);

        given(authService.authUser(Mockito.any())).willReturn(true);
        given(userService.getUserById(1L)).willReturn(user);

        // Authenticated: should return MyUserDTO (includes email)
        mockMvc.perform(get("/users/1")
                        .header("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("testUser")))
                .andExpect(jsonPath("$.email", is("test@test.com")));
    }

    @Test
    public void getUser_notAuthenticated_returnsUserDTO() throws Exception {
        UserScoreboard scoreboard = new UserScoreboard();
        scoreboard.setTotalPoints(0);
        scoreboard.setGamesPlayed(0);
        scoreboard.setGamesWon(0);
        scoreboard.setGuessingPrecision(0f);

        User user = new User();
        user.setUserId(1L);
        user.setUsername("testUser");
        user.setEmail("test@test.com");
        user.setUserScoreboard(scoreboard);

        given(authService.authUser(Mockito.any())).willReturn(false);
        given(userService.getUserById(1L)).willReturn(user);

        // Not authenticated: should return UserDTO (no email field)
        mockMvc.perform(get("/users/1")
                        .header("token", "wrong-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("testUser")));
    }

    @Test
    public void getUser_notFound_returnsNotFound() throws Exception {
        // authService throws NOT_FOUND when user ID doesn't exist in the database
        given(authService.authUser(Mockito.any()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "This user could not be found"));

        mockMvc.perform(get("/users/99")
                        .header("token", "some-token"))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    // POST /users/{userId}/logout
    // =========================================================

    @Test
    public void logoutUser_validToken_returnsOk() throws Exception {
        given(authService.authUser(Mockito.any())).willReturn(true);
        doNothing().when(userService).logoutUser(Mockito.any());

        mockMvc.perform(post("/users/1/logout")
                        .header("token", "valid-token"))
                .andExpect(status().isOk());
    }

    @Test
    public void logoutUser_invalidToken_returnsUnauthorized() throws Exception {
        given(authService.authUser(Mockito.any())).willReturn(false);

        mockMvc.perform(post("/users/1/logout")
                        .header("token", "wrong-token"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================
    // PUT /users/{userId}
    // =========================================================

    @Test
    public void updateUser_validInput_returnsNoContent() throws Exception {
        UpdateUserPutDTO updateUserPutDTO = new UpdateUserPutDTO();
        updateUserPutDTO.setUsername("newUsername");
        updateUserPutDTO.setUserBio("new bio");

        given(authService.authUser(Mockito.any())).willReturn(true);
        doNothing().when(userService).updateUser(Mockito.any(), Mockito.any());

        mockMvc.perform(put("/users/1")
                        .header("token", "valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(updateUserPutDTO)))
                .andExpect(status().isNoContent());
    }

    @Test
    public void updateUser_invalidToken_returnsUnauthorized() throws Exception {
        UpdateUserPutDTO updateUserPutDTO = new UpdateUserPutDTO();
        updateUserPutDTO.setUsername("newUsername");

        given(authService.authUser(Mockito.any())).willReturn(false);

        mockMvc.perform(put("/users/1")
                        .header("token", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(updateUserPutDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void updateUser_userNotFound_returnsNotFound() throws Exception {
        UpdateUserPutDTO updateUserPutDTO = new UpdateUserPutDTO();
        updateUserPutDTO.setUsername("newUsername");

        given(authService.authUser(Mockito.any())).willReturn(true);
        Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .when(userService).updateUser(Mockito.any(), Mockito.any());

        mockMvc.perform(put("/users/1")
                        .header("token", "valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(updateUserPutDTO)))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    // Helper
    // =========================================================

    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e.toString()));
        }
    }
}
