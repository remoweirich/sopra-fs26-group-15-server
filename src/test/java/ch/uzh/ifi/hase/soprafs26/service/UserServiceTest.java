package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UserService userService;

	private User testUser;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);

		// given
		testUser = new User();
		testUser.setUserId(1L);
		testUser.setUsername("testUsername");
		testUser.setEmail("test@uzh.ch");
		testUser.setPassword("password");

		// when -> any object is being save in the userRepository -> return the dummy
		// testUser
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
	}

	@Test
	public void registerUser_validInputs_success() {
		// when -> any object is being save in the userRepository -> return the dummy
		User createdUser = userService.registerUser(testUser);

		// then
		Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());

		assertEquals(testUser.getUserId(), createdUser.getUserId());
		assertEquals(testUser.getUsername(), createdUser.getUsername());
		assertNotNull(createdUser.getCreationDate());
		assertNull(createdUser.getToken());
		assertNotNull(createdUser.getUserScoreboard());
		assertEquals(UserStatus.OFFLINE, createdUser.getStatus());
	}

	@Test
	public void registerUser_duplicateUsername_throwsException() {
		// when -> setup additional mocks for UserRepository
		Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

		// then -> attempt to create second user with same user -> check that an error
		// is thrown
		assertThrows(ResponseStatusException.class, () -> userService.registerUser(testUser));
	}

	@Test
	public void registerUser_duplicateEmail_throwsException() {
		// when -> setup additional mocks for UserRepository
		Mockito.when(userRepository.findByEmail(Mockito.any())).thenReturn(testUser);

		// then -> attempt to create second user with same user -> check that an error
		// is thrown
		assertThrows(ResponseStatusException.class, () -> userService.registerUser(testUser));
	}

}
