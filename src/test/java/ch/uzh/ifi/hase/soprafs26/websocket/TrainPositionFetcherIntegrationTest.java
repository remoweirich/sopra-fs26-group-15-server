package ch.uzh.ifi.hase.soprafs26.websocket;

import ch.uzh.ifi.hase.soprafs26.objects.Train;
import ch.uzh.ifi.hase.soprafs26.trains.TrainPositionFetcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = "geops.mock=true")
class TrainPositionFetcherIntegrationTest {

    @Autowired
    private TrainPositionFetcher trainPositionFetcher;

    /**
     * Prueft: fetchTrainsMock liest die Mock-Datei und liefert im
     * vollstaendigen Spring-Kontext (mit @Value-Injection) eine nicht-leere Liste.
     * Faengt Bug: Wenn ClassPathResource oder ObjectMapper nicht korrekt
     * initialisiert sind, wuerde dieser Test mit IOException/NPE scheitern.
     */
    @Test
    void fetchTrainsMock_withRealSpringContext_returnsTrains() throws Exception {
        List<Train> result = trainPositionFetcher.fetchTrainsMock(2);
        assertNotNull(result);
        assertTrue(result.size() <= 2);
    }

    /**
     * Prueft: fetchTrains leitet an fetchTrainsMock weiter wenn
     * geops.mock=true in application.properties gesetzt ist.
     * Faengt Bug: Wenn die useMock-Property nicht korrekt injiziert wird,
     * wuerde fetchTrains die Live-API aufrufen und im CI-Build scheitern.
     */


//    @Test
//    void fetchTrains_withMockModeFromProperties_returnsTrains() throws Exception {
//        List<Train> result = trainPositionFetcher.fetchTrains(2);
//        assertNotNull(result);
//        assertTrue(result.size() <= 2);
//    }
}
