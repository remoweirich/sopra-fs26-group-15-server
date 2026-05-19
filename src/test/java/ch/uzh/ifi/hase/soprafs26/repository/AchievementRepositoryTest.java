package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.Achievement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class AchievementRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AchievementRepository achievementRepository;

    // ═══════════════════════════════════════════════════════════════════
    // findByName
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: Ein persistiertes Achievement wird ueber seinen Namen gefunden.
     * Faengt Bug: AchievementService nutzt findByName() um zu pruefen, ob ein
     * Achievement schon existiert, bevor es vergeben wird — ein kaputter Query
     * wuerde Duplikate anlegen oder Vergabe-Checks ueberspringen.
     */
    @Test
    public void findByName_existingAchievement_returnsAchievement() {
        Achievement a = new Achievement();
        a.setName("First Guess");
        a.setDescription("Made your first guess");
        a.setIconUrl("icons/first_guess.png");
        entityManager.persist(a);
        entityManager.flush();

        Achievement found = achievementRepository.findByName("First Guess");

        assertNotNull(found);
        assertEquals(a.getAchievementId(), found.getAchievementId());
        assertEquals("First Guess", found.getName());
        assertEquals("Made your first guess", found.getDescription());
    }

    /**
     * Prueft: Unbekannter Name liefert null.
     * Faengt Bug: Wenn immer ein Objekt zurueckgegeben wuerde, wuerde die
     * Existenzpruefung nie greifen und jede Achievement-Vergabe waere eine Duplikat.
     */
    @Test
    public void findByName_unknownName_returnsNull() {
        Achievement found = achievementRepository.findByName("Nonexistent Badge");

        assertNull(found);
    }

    /**
     * Prueft: Namensabfrage ist case-sensitive (Groß-/Kleinschreibung wird unterschieden).
     * Faengt Bug: Eine case-insensitive Konfiguration wuerde "first guess" und
     * "First Guess" als gleich behandeln — falsche Achievement-Matches.
     */
    @Test
    public void findByName_wrongCase_returnsNull() {
        Achievement a = new Achievement();
        a.setName("Precision Master");
        entityManager.persist(a);
        entityManager.flush();

        Achievement found = achievementRepository.findByName("precision master");

        assertNull(found, "findByName must be case-sensitive");
    }
}
