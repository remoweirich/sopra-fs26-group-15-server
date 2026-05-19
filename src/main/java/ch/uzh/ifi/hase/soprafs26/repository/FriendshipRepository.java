package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.Friendship;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    Optional<Friendship> findByFriend1AndFriend2(User u1, User u2);


    Optional<Friendship> findByFriendshipId(Long friendshipId);


    @Query("SELECT f FROM Friendship f WHERE (f.friend1 = :user OR f.friend2 = :user) " +
            "AND f.pendingInvitationReceivedBy IS NULL")
    List<Friendship> findAllByUser(@Param("user") User user);


    @Query("SELECT f FROM Friendship f WHERE f.pendingInvitationReceivedBy = :user")
    List<Friendship> findAllPendingReceivedByUser(@Param("user") User user);


    @Query("SELECT f FROM Friendship f WHERE (f.friend1 = :user OR f.friend2 = :user) " +
            "AND (f.pendingInvitationReceivedBy != :user)" +
            "AND (f.pendingInvitationReceivedBy IS NOT NULL)")
    List<Friendship> findAllPendingSentByUser(@Param("user") User user);

    void deleteByFriendshipId(Long friendshipId);
}
