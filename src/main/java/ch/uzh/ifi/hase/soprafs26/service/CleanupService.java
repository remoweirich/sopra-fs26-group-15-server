package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.RoundHistoryRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CleanupService {
    private final UserRepository userRepository;
    private final LobbyRepository lobbyRepository;
    private final RoundHistoryRepository roundHistoryRepository;

    public CleanupService(UserRepository userRepository, LobbyRepository lobbyRepository,  RoundHistoryRepository roundHistoryRepository) {
        this.userRepository = userRepository;
        this.lobbyRepository = lobbyRepository;
        this.roundHistoryRepository = roundHistoryRepository;

    }

    @Value("${cleanup.guest.cutoff}")
    private long guestCutoffMinutes;

    @Value("${cleanup.lobby.cutoff}")
    private long lobbyCutoffMinutes;

    @Value("${cleanup.finished.cutoff}")
    private long finishedCutoffMinutes;


    @Scheduled(fixedRateString = "${cleanup.guest.rate}")
    public void deleteOrphanedGuestUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(guestCutoffMinutes);

        List<Long> occupiedUserIds = lobbyRepository.findAll().stream()
                .filter(l -> l.getLobbyState() != LobbyState.FINISHED || Boolean.TRUE.equals(l.getCleanupPending()))
                .flatMap(l -> l.getPlayers().stream())
                .map(User::getUserId)
                .collect(Collectors.toList());

        List<User> allGuests = userRepository.findAllGuests();
        List<User> guestsToDelete = allGuests.stream()
                .filter(g -> !occupiedUserIds.contains(g.getUserId()))
                .filter(g -> !g.getUserProfile().getUsername().equals("KingBabaBui"))
                .filter(g -> g.getCreationDate() != null && g.getCreationDate().isBefore(cutoff))
                .collect(Collectors.toList());
        System.out.println("Cleanup running – deleting " + guestsToDelete.size() + " orphaned guests");
        guestsToDelete.forEach(userRepository::delete);
    }

    @Scheduled(fixedRateString = "${cleanup.lobby.rate}")
    public void deleteOrphanedLobbies() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(lobbyCutoffMinutes);
        List<Lobby> oldLobbies = lobbyRepository.findAll().stream()
                .filter(l -> l.getCreationDate() != null && l.getCreationDate().isBefore(cutoff))
                .filter(l -> l.getLobbyState() == LobbyState.WAITING)

                .collect(Collectors.toList());
        System.out.println("Cleanup running – deleting " + oldLobbies.size() + " orphaned lobbies");
        oldLobbies.forEach(lobbyRepository::delete);
    }

    @Scheduled(fixedRateString = "${cleanup.finished.rate}")
    public void cleanupFinishedLobbies() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(finishedCutoffMinutes);
        User king = userRepository.findByUserProfileUsername("KingBabaBui");
        lobbyRepository.findAll().stream()
                .filter(l -> l.getLobbyState() == LobbyState.FINISHED)
                .filter(l -> Boolean.TRUE.equals(l.getCleanupPending()))
                .filter(l -> l.getCreationDate() != null && l.getCreationDate().isBefore(cutoff))
                .forEach(lobby -> {

                    if (king != null && lobby.getAdmin().getIsGuest()) {
                        lobby.setAdmin(king);
                    }

                    if (king != null && lobby.getWinner() != null && lobby.getWinner().getIsGuest()) {
                        lobby.setWinner(king);
                    }

                    lobby.getPlayers().removeIf(p ->
                            p.getIsGuest() && !p.getUserProfile().getUsername().equals("KingBabaBui"));

                    if (king != null) {
                        roundHistoryRepository.findAll().stream()
                                .filter(rh -> rh.getLobby().getLobbyId().equals(lobby.getLobbyId()))
                                .filter(rh -> rh.getUser().getIsGuest())
                                .forEach(rh -> {
                                    rh.setUser(king);
                                    roundHistoryRepository.save(rh);
                                });
                    }

                    lobby.setCleanupPending(false);
                    lobbyRepository.save(lobby);
                });
    }
}
