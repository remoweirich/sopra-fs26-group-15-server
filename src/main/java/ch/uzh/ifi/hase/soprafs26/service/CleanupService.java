package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
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


    @Scheduled(fixedRate = 30_000)
    public void deleteOrphanedGuestUsers() {

        List<Long> occupiedUserIds = lobbyRepository.findAll().stream()
                .filter(l -> l.getLobbyState() != LobbyState.FINISHED)
                .flatMap(l -> l.getPlayers().stream())
                .map(User::getUserId)
                .collect(Collectors.toList());

        List<User> allGuests = userRepository.findAllGuests();
        List<User> guestsToDelete = allGuests.stream()
                .filter(g -> !occupiedUserIds.contains(g.getUserId()))
                .filter(g -> !g.getUserProfile().getUsername().equals("KingBabaBui"))
                .collect(Collectors.toList());
        System.out.println("Cleanup running – deleting " + guestsToDelete.size() + " orphaned guests");
        guestsToDelete.forEach(userRepository::delete);
    }

    @Scheduled(fixedRate = 30_000)
    public void deleteOrphanedLobbies() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(1);
        List<Lobby> oldLobbies = lobbyRepository.findAll().stream()
                .filter(l -> l.getCreationDate() != null && l.getCreationDate().isBefore(cutoff))
                .filter(l -> l.getLobbyState() == LobbyState.WAITING)
                .collect(Collectors.toList());
        System.out.println("Cleanup running – deleting " + oldLobbies.size() + " orphaned lobbies");
        oldLobbies.forEach(lobbyRepository::delete);
    }
}
