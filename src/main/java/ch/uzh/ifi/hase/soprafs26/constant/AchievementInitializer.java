package ch.uzh.ifi.hase.soprafs26.constant;

import ch.uzh.ifi.hase.soprafs26.entity.Achievement;
import ch.uzh.ifi.hase.soprafs26.repository.AchievementRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

@Component
public class AchievementInitializer {

    private final AchievementRepository achievementRepository;

    public AchievementInitializer(AchievementRepository achievementRepository) {
        this.achievementRepository = achievementRepository;
    }

    @PostConstruct
    @Transactional
    public void initializeAchievements() {
        if (achievementRepository.count() == 0) {
            Achievement achievement1 = new Achievement();
            achievement1.setName("Rookie Traveler");
            achievement1.setDescription("Complete your first game.");
            achievement1.setIconUrl("https://cdn-icons-png.flaticon.com/512/3132/3132693.png");
            achievementRepository.save(achievement1);

            Achievement achievement2 = new Achievement();
            achievement2.setName("Seasoned Traveler");
            achievement2.setDescription("Complete 10 games.");
            achievement2.setIconUrl("https://cdn-icons-png.flaticon.com/512/2830/2830284.png");
            achievementRepository.save(achievement2);

            Achievement achievement3 = new Achievement();
            achievement3.setName("Close Call");
            achievement3.setDescription("Guess within 0.5 km of a train.");
            achievement3.setIconUrl("https://cdn-icons-png.flaticon.com/512/1828/1828640.png");
            achievementRepository.save(achievement3);

            Achievement achievement4 = new Achievement();
            achievement4.setName("Perfect Round");
            achievement4.setDescription("Score 1000 points in a single round.");
            achievement4.setIconUrl("https://cdn-icons-png.flaticon.com/512/1041/1041728.png");
            achievementRepository.save(achievement4);

            Achievement achievement5 = new Achievement();
            achievement5.setName("Train Transfer");
            achievement5.setDescription("Score over 800 points in 5 consecutive rounds.");
            achievement5.setIconUrl("https://cdn-icons-png.flaticon.com/512/3176/3176392.png");
            achievementRepository.save(achievement5);

            Achievement achievement6 = new Achievement();
            achievement6.setName("Conductor");
            achievement6.setDescription("Win a multiplayer game.");
            achievement6.setIconUrl("https://cdn-icons-png.flaticon.com/512/3063/3063825.png");
            achievementRepository.save(achievement6);

            Achievement achievement7 = new Achievement();
            achievement7.setName("Emergency Stop");
            achievement7.setDescription("Get 0 Points in a round.");
            achievement7.setIconUrl("https://cdn-icons-png.flaticon.com/512/1946/1946429.png");
            achievementRepository.save(achievement7);

            Achievement achievement8 = new Achievement();
            achievement8.setName("Swiss Rail Expert");
            achievement8.setDescription("Accumulate 10,000 total points across all games.");
            achievement8.setIconUrl("https://cdn-icons-png.flaticon.com/512/2942/2942909.png");
            achievementRepository.save(achievement8);

            Achievement achievement9 = new Achievement();
            achievement9.setName("Frequent Flyer");
            achievement9.setDescription("Play 50 rounds in total.");
            achievement9.setIconUrl("https://cdn-icons-png.flaticon.com/512/870/870160.png");
            achievementRepository.save(achievement9);

            Achievement achievement10 = new Achievement();
            achievement10.setName("Wrong Train!");
            achievement10.setDescription("Guess more than 100 km away from the actual train.");
            achievement10.setIconUrl("https://cdn-icons-png.flaticon.com/512/3480/3480289.png");
            achievementRepository.save(achievement10);
        }
    }
}
