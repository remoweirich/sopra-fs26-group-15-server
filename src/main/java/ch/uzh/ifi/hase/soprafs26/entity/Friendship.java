package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "friendship", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"idFriend1", "idFriend2"})
})
@Getter
@Setter
@NoArgsConstructor
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "idFriend1", nullable = false)
    private User friend1;

    @ManyToOne
    @JoinColumn(name = "idFriend2", nullable = false)
    private User friend2;

    @ManyToOne
    @JoinColumn(name = "pendingInvitationReceivedBy")
    private User pendingInvitationReceivedBy;
}