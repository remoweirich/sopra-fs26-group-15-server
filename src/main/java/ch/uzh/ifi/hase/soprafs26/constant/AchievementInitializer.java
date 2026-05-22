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
            achievement1.setName("Streckenbillet");
            achievement1.setDescription("Spiele deine erste Runde.");
            achievement1.setIconUrl("/achievements/rookie-traveler.svg");
            achievementRepository.save(achievement1);

            Achievement achievement2 = new Achievement();
            achievement2.setName("Dauerpendler");
            achievement2.setDescription("Spiele 10 mal");
            achievement2.setIconUrl("/achievements/seasoned-traveler.svg");
            achievementRepository.save(achievement2);

            Achievement achievement3 = new Achievement();
            achievement3.setName("Haarscharf vorbei");
            achievement3.setDescription("Weniger als 0,5 km neben dem Zug getippt.");
            achievement3.setIconUrl("/achievements/close-call.svg");
            achievementRepository.save(achievement3);

            Achievement achievement4 = new Achievement();
            achievement4.setName("Pünktlicher als die SBB");
            achievement4.setDescription("1000 Punkte in einer einzigen Runde geholt. Absolute Perfektion.");
            achievement4.setIconUrl("/achievements/perfect-round.svg");
            achievementRepository.save(achievement4);

            Achievement achievement5 = new Achievement();
            achievement5.setName("Anschlusszug erwischt");
            achievement5.setDescription("Über 800 Punkte in 5 aufeinanderfolgenden Runden gescort.");
            achievement5.setIconUrl("/achievements/train-transfer.svg");
            achievementRepository.save(achievement5);

            Achievement achievement6 = new Achievement();
            achievement6.setName("Lokführer");
            achievement6.setDescription("Ein Multiplayer-Spiel gewonnen.");
            achievement6.setIconUrl("/achievements/conductor.svg");
            achievementRepository.save(achievement6);

            Achievement achievement7 = new Achievement();
            achievement7.setName("Notbremse");
            achievement7.setDescription("0 Punkte in einer Runde eingefahren.");
            achievement7.setIconUrl("/achievements/emergency-stop.svg");
            achievementRepository.save(achievement7);

            Achievement achievement8 = new Achievement();
            achievement8.setName("GA-Besitzer");
            achievement8.setDescription("Insgesamt 10.000 Punkte gesammelt.");
            achievement8.setIconUrl("/achievements/swiss-rail-expert.svg");
            achievementRepository.save(achievement8);

            Achievement achievement9 = new Achievement();
            achievement9.setName("Meilen-Millionär");
            achievement9.setDescription("Insgesamt 50 Runden gespielt.");
            achievement9.setIconUrl("/achievements/frequent-flyer.svg");
            achievementRepository.save(achievement9);

            Achievement achievement10 = new Achievement();
            achievement10.setName("Im falschen Film... und Zug!");
            achievement10.setDescription("Mehr als 100 km daneben getippt.");
            achievement10.setIconUrl("/achievements/wrong-train.svg");
            achievementRepository.save(achievement10);

            Achievement achievement11 = new Achievement();
            achievement11.setName("King BabaBui");
            achievement11.setDescription("Lang lebe der König!");
            achievement11.setIconUrl("/achievements/king-bababui.svg");
            achievementRepository.save(achievement11);
        }
    }
}
