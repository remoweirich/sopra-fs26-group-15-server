package ch.uzh.ifi.hase.soprafs26.security;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Boolean authUser(AuthHeader authHeader) {
        User user = userRepository.findById(Long.parseLong(authHeader.getUserId())).orElse(null);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This user could not be found");
        }
        if (user.getToken() == null) {
            return false;
        }
        return user.getToken().equals(authHeader.getToken());
    }
}
