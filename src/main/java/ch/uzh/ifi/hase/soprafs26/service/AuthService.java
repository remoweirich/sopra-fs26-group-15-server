package ch.uzh.ifi.hase.soprafs26.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    public AuthService() {
        // Constructor for AuthService
    }

    public void authUser(String token, String userId) {
        // Implement your authentication logic here
        // For example, you can check if the token is valid and return true or false
        // accordingly
    }
}
