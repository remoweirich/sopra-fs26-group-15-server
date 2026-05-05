package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.RoundHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoundHistoryRepository extends JpaRepository<RoundHistory, Long> {
    List<RoundHistory> findByUserUserId(Long userId);
    List<RoundHistory> findByLobbyLobbyId(Long lobbyId);
}