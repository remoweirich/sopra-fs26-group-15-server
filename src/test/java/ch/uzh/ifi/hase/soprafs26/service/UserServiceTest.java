package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    @Test
    void getUsers() {
    }

    @Test
    void registerUser() {
    }

    @Test
    void loginUser() {
    }

    @Test
    void getUserById() {
    }

    @Test
    void logoutUser() {
    }

    @Test
    void updateUser() {
    }
}




// INTEGRATION TEST
//
//package ch.uzh.ifi.hase.soprafs26.service;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.web.WebAppConfiguration;
//import org.springframework.web.server.ResponseStatusException;
//
//import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
//import ch.uzh.ifi.hase.soprafs26.entity.User;
//import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * Test class for the UserResource REST resource.
// *
// * @see UserService
// */
//@WebAppConfiguration
//@SpringBootTest
//public class UserServiceIntegrationTest {
//
//    @Qualifier("userRepository")
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private UserService userService;
//
//    @BeforeEach
//    public void setup() {
//        userRepository.deleteAll();
//    }
//
//    @Test
//    public void createUser_validInputs_success() {
//        // given
//        assertNull(userRepository.findByUsername("testUsername"));
//
//        User testUser = new User();
//        testUser.setUsername("testUsername");
//
//        // when
//        User createdUser = userService.createUser(testUser);
//
//        // then
//        assertEquals(testUser.getUserId(), createdUser.getUserId());
//        assertEquals(testUser.getUsername(), createdUser.getUsername());
//        assertNotNull(createdUser.getToken());
//        assertEquals(UserStatus.OFFLINE, createdUser.getStatus());
//    }
//
//    @Test
//    public void createUser_duplicateUsername_throwsException() {
//        assertNull(userRepository.findByUsername("testUsername"));
//
//        User testUser = new User();
//        testUser.setUsername("testUsername");
//        userService.createUser(testUser);
//
//        // attempt to create second user with same username
//        User testUser2 = new User();
//
//        // change the name but forget about the username
//        testUser2.setUsername("testUsername");
//
//        // check that an error is thrown
//        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser2));
//    }
//}

















// UNIT TEST

//package ch.uzh.ifi.hase.soprafs26.service;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.MockitoAnnotations;
//import org.springframework.web.server.ResponseStatusException;
//
//import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
//import ch.uzh.ifi.hase.soprafs26.entity.User;
//import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//public class UserServiceTest {
//
//    @Mock
//    private UserRepository userRepository;
//
//    @InjectMocks
//    private UserService userService;
//
//    private User testUser;
//
//    @BeforeEach
//    public void setup() {
//        MockitoAnnotations.openMocks(this);
//
//        // given
//        testUser = new User();
//        testUser.setUserId(1L);
//        testUser.setUsername("testUsername");
//
//        // when -> any object is being save in the userRepository -> return the dummy
//        // testUser
//        Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
//    }
//
//    @Test
//    public void createUser_validInputs_success() {
//        // when -> any object is being save in the userRepository -> return the dummy
//        // testUser
//        User createdUser = userService.createUser(testUser);
//
//        // then
//        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());
//
//        assertEquals(testUser.getUserId(), createdUser.getUserId());
//        assertEquals(testUser.getUsername(), createdUser.getUsername());
//        assertNotNull(createdUser.getToken());
//        assertEquals(UserStatus.OFFLINE, createdUser.getStatus());
//    }
//
//    @Test
//    public void createUser_duplicateName_throwsException() {
//        // given -> a first user has already been created
//        userService.createUser(testUser);
//
//        // when -> setup additional mocks for UserRepository
//        Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(null);
//
//        // then -> attempt to create second user with same user -> check that an error
//        // is thrown
//        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
//    }
//
//    @Test
//    public void createUser_duplicateInputs_throwsException() {
//        // given -> a first user has already been created
//        userService.createUser(testUser);
//
//        // when -> setup additional mocks for UserRepository
//        Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);
//
//        // then -> attempt to create second user with same user -> check that an error
//        // is thrown
//        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
//    }
//
//}

