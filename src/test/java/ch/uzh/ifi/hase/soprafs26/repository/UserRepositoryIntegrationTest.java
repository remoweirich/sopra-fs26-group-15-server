package ch.uzh.ifi.hase.soprafs26.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
public class UserRepositoryIntegrationTest {

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private UserRepository userRepository;

	@Test
	public void findById_success() {
		// given
		User user = new User();
		user.setUsername("firstname@lastname");
		user.setEmail("firstname@lastname.ch");
		user.setPassword("password");
		user.setStatus(UserStatus.OFFLINE);
		user.setToken("1");

		entityManager.persist(user);
		entityManager.flush();

		// when
		User found = userRepository.findById(user.getUserId()).orElse(null);

		// then
		assertNotNull(found.getUserId());
		assertEquals(found.getUsername(), user.getUsername());
		assertEquals(found.getToken(), user.getToken());
		assertEquals(found.getStatus(), user.getStatus());
	}
}
