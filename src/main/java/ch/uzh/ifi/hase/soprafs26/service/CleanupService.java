package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CleanupService {
    private final UserRepository userRepository;
    private final LobbyRepository lobbyRepository;

    public CleanupService(UserRepository userRepository, LobbyRepository lobbyRepository) {
        this.userRepository = userRepository;
        this.lobbyRepository = lobbyRepository;
    }

    @Scheduled(fixedRate = 10_000)
    public void deleteOrphanedGuestUsers() {
        List<User> allGuests = userRepository.findAllGuests();

        List<Long> occupiedUserIds = lobbyRepository.findAll().stream()
                .flatMap(l -> l.getPlayers().stream())
                .map(User::getUserId)
                .collect(Collectors.toList());

        List<User> guestsToDelete = allGuests.stream()
                .filter(g -> !occupiedUserIds.contains(g.getUserId()))
                .collect(Collectors.toList());

        System.out.println("Cleanup running – deleting " + guestsToDelete.size() + " orphaned guests");
        guestsToDelete.forEach(userRepository::delete);
    }
}
