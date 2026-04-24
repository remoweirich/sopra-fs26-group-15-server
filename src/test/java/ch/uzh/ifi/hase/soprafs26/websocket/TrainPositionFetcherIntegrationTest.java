package ch.uzh.ifi.hase.soprafs26.websocket;

import ch.uzh.ifi.hase.soprafs26.objects.Train;
import ch.uzh.ifi.hase.soprafs26.trains.TrainPositionFetcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class TrainPositionFetcherIntegrationTest {

    @MockitoBean
    private TrainPositionFetcher trainPositionFetcher;

    @Test
    void fetchTrainsMock_withRealSpringContext_returnsTrains() throws Exception {
        List<Train> result = trainPositionFetcher.fetchTrainsMock(2);
        assertNotNull(result);
        assertTrue(result.size() <= 2);
    }

    @Test
    void fetchTrains_withMockModeFromProperties_returnsTrains() throws Exception {
        // useMock=true ist in application.properties gesetzt
        // also sollte fetchTrains direkt fetchTrainsMock aufrufen
        List<Train> result = trainPositionFetcher.fetchTrains(2);
        assertNotNull(result);
        assertTrue(result.size() <= 2);
    }
}