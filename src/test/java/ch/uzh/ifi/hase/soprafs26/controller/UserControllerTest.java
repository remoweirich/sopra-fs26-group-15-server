package ch.uzh.ifi.hase.soprafs26.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LoginPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.RegisterPostDTO;
import ch.uzh.ifi.hase.soprafs26.security.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the UserController works.
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private AuthService authService;

	@Test
	public void registerUser_validInput_userCreated() throws Exception {
		// given
		User user = new User();
		user.setUserId(1L);
		user.setUsername("testUsername");
		user.setEmail("test@uzh.ch");
		user.setPassword("password");
		user.setToken(null);
		user.setStatus(UserStatus.OFFLINE);

		RegisterPostDTO registerPostDTO = new RegisterPostDTO();
		registerPostDTO.setUsername("testUsername");
		registerPostDTO.setEmail("test@uzh.ch");
		registerPostDTO.setPassword("password");

		given(userService.registerUser(Mockito.any())).willReturn(user);

		MockHttpServletRequestBuilder postRequest = post("/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(registerPostDTO));

		// then
		mockMvc.perform(postRequest)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId", is(user.getUserId().intValue())))
				.andExpect(jsonPath("$.token").doesNotExist());
	}

	@Test
	public void loginUser_validInput_userAuthenticated() throws Exception {
		// given
		User user = new User();
		user.setUserId(1L);
		user.setUsername("testUsername");
		user.setPassword("password");
		user.setToken("1");
		user.setStatus(UserStatus.ONLINE);

		LoginPostDTO loginPostDTO = new LoginPostDTO();
		loginPostDTO.setUsername("testUsername");
		loginPostDTO.setPassword("password");

		given(userService.loginUser(Mockito.anyString(), Mockito.anyString())).willReturn(user);

		// when/then -> do the request + validate the result
		MockHttpServletRequestBuilder postRequest = post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(loginPostDTO));

		// then
		mockMvc.perform(postRequest)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId", is(user.getUserId().intValue())))
				.andExpect(jsonPath("$.token", is(user.getToken())));
	}

	/**
	 * Helper Method to convert userPostDTO into a JSON string such that the input
	 * can be processed
	 * Input will look like this: {"name": "Test User", "username": "testUsername"}
	 * 
	 * @param object
	 * @return string
	 */
	private String asJsonString(final Object object) {
		try {
			return new ObjectMapper().writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					String.format("The request body could not be created.%s", e.toString()));
		}
	}
}