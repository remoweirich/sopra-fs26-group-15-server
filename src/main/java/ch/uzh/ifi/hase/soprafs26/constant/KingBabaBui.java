package ch.uzh.ifi.hase.soprafs26.constant;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class KingBabaBui {
    private final UserRepository userRepository;
    private final UserService userService;

    public KingBabaBui(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @PostConstruct
    @Transactional
    public void initializeSystemUser() {
        if (userRepository.findByUserProfileUsername("KingBabaBui") == null) {
            User king = new User();
            UserProfile profile = new UserProfile();
            profile.setUsername("KingBabaBui");
            profile.setPassword(UUID.randomUUID().toString());
            profile.setEmail("kingbababui@system.com");
            king.setUserProfile(profile);
            king.setIsGuest(true);
            userService.registerUser(king);
        }
    }
}
