package ch.uzh.ifi.hase.soprafs26.trains;

import ch.uzh.ifi.hase.soprafs26.objects.LineString;
import ch.uzh.ifi.hase.soprafs26.objects.Station;
import ch.uzh.ifi.hase.soprafs26.objects.Train;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

class TrainPositionFetcherTest {

    private TrainPositionFetcher trainPositionFetcher;

    @BeforeEach
    void setUp() {
        trainPositionFetcher = new TrainPositionFetcher();
    }

    // ═══════════════════════════════════════════════════════════════════
    // fetchTrains — routing
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: fetchTrains delegiert an fetchTrainsMock wenn useMock=true.
     * Faengt Bug: Wenn die Bedingung invertiert ist, wuerden Produktion-
     * Netzwerkaufrufe in der Test-/Mock-Umgebung stattfinden.
     */
    @Test
    void fetchTrains_mockModeEnabled_callsFetchTrainsMock() throws Exception {
        TrainPositionFetcher spy = Mockito.spy(new TrainPositionFetcher());
        ReflectionTestUtils.setField(spy, "useMock", true);

        List<Train> mockTrains = List.of(new Train("1"));
        doReturn(mockTrains).when(spy).fetchTrainsMock(1);

        List<Train> result = spy.fetchTrains(1);

        assertEquals(mockTrains, result);
    }

    /**
     * Prueft: fetchTrains delegiert an fetchTrainsLive wenn useMock=false.
     */
    @Test
    void fetchTrains_liveModeEnabled_callsFetchTrainsLive() throws Exception {
        TrainPositionFetcher spy = Mockito.spy(new TrainPositionFetcher());
        ReflectionTestUtils.setField(spy, "useMock", false);

        List<Train> liveTrains = List.of(new Train("1"));
        doReturn(liveTrains).when(spy).fetchTrainsLive(1);

        List<Train> result = spy.fetchTrains(1);

        assertEquals(liveTrains, result);
    }

    // ═══════════════════════════════════════════════════════════════════
    // fetchTrainsMock — basic contract
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: fetchTrainsMock gibt nie null zurueck.
     */
    @Test
    void fetchTrainsMock_returnsNonNullList() throws Exception {
        List<Train> result = trainPositionFetcher.fetchTrainsMock(2);
        assertNotNull(result);
    }

    /**
     * Prueft: fetchTrainsMock ueberschreitet den angeforderten subsetSize nicht.
     * Faengt Bug: Ein fehlender subList-Aufruf wuerde alle Zuege zurueckgeben,
     * egal welchen subsetSize der Caller angefordert hat.
     */
    @Test
    void fetchTrainsMock_respectsSubsetSize() throws Exception {
        List<Train> result = trainPositionFetcher.fetchTrainsMock(2);
        assertTrue(result.size() <= 2);
    }

    /**
     * Prueft: Die zurueckgegebenen Zuege haben mindestens eine trainId.
     * Faengt Bug: Wenn das Parsen der trajectoryMessages fehlschlaegt,
     * wuerde ein Zug ohne ID an GameService weitergegeben und beim
     * Serialisieren einen NPE ausloesen.
     */
    @Test
    void fetchTrainsMock_returnsEnrichedTrains() throws Exception {
        List<Train> result = trainPositionFetcher.fetchTrainsMock(1);
        assertFalse(result.isEmpty());
        assertNotNull(result.get(0).getTrainId());
    }

    /**
     * Prueft: subsetSize=0 ergibt eine leere Liste (kein Absturz).
     */
    @Test
    void fetchTrainsMock_subsetSizeZero_returnsEmptyList() throws Exception {
        List<Train> result = trainPositionFetcher.fetchTrainsMock(0);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ═══════════════════════════════════════════════════════════════════
    // interpolatePosition
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Prueft: Bei timestamp=50 zwischen departure=0 und arrival=100
     * landet der Zug exakt in der Mitte des Segments (50, 50).
     * Faengt Bug: Ein falscher progressRatio-Ausdruck (z.B. total/elapsed)
     * wuerde den Zug ans falsche Ende des Segments setzen.
     */
    @Test
    void interpolatePosition_midway_returnsMiddlePoint() {
        Train train = new Train("1");
        train.setLastLeavingStation(new Station("A", 0, 0, 0, 100));
        train.setNextPendingStation(new Station("B", 100, 100, 0, 100));
        train.setTimestamp(50);
        train.setLineString(new LineString(List.of(
                new LineString.Point(0, 0),
                new LineString.Point(100, 100)
        )));

        trainPositionFetcher.interpolatePosition(train);

        assertEquals(50, train.getCurrentX());
        assertEquals(50, train.getCurrentY());
    }

    /**
     * Prueft: Ein leerer LineString veranlasst snap zur letzten bekannten Station.
     * Faengt Bug: Wenn die leere-Liste-Pruefung fehlt, wuerden nachfolgende
     * Segment-Iterationen einen IndexOutOfBoundsException ausloesen.
     */
    @Test
    void interpolatePosition_emptyLineString_snapsToLastStation() {
        Train train = new Train("1");
        train.setLastLeavingStation(new Station("A", 42, 99, 0, 100));
        train.setNextPendingStation(new Station("B", 100, 100, 0, 100));
        train.setTimestamp(50);
        train.setLineString(new LineString(new ArrayList<>()));

        trainPositionFetcher.interpolatePosition(train);

        assertEquals(42, train.getCurrentX());
        assertEquals(99, train.getCurrentY());
    }

    /**
     * Prueft: timestamp vor departureTime wird auf den Startpunkt geclampst.
     * Faengt Bug: Ohne Clamp wuerde ein negativer progressRatio den Zug
     * vor dem Startpunkt des Segments platzieren.
     */
    @Test
    void interpolatePosition_timestampBeforeDeparture_snapsToStart() {
        Train train = new Train("1");
        train.setLastLeavingStation(new Station("A", 0, 0, 100, 200));
        train.setNextPendingStation(new Station("B", 100, 100, 100, 200));
        train.setTimestamp(50);
        train.setLineString(new LineString(List.of(
                new LineString.Point(0, 0),
                new LineString.Point(100, 100)
        )));

        trainPositionFetcher.interpolatePosition(train);

        assertEquals(0, train.getCurrentX());
        assertEquals(0, train.getCurrentY());
    }

    /**
     * Prueft: timestamp nach arrivalTime wird auf den Endpunkt geclampst.
     * Faengt Bug: Ohne Clamp wuerde progressRatio > 1 den Zug jenseits
     * des Endpunkts setzen.
     */
    @Test
    void interpolatePosition_timestampAfterArrival_snapsToEnd() {
        Train train = new Train("1");
        train.setLastLeavingStation(new Station("A", 0, 0, 0, 100));
        train.setNextPendingStation(new Station("B", 100, 100, 0, 100));
        train.setTimestamp(200);
        train.setLineString(new LineString(List.of(
                new LineString.Point(0, 0),
                new LineString.Point(100, 100)
        )));

        trainPositionFetcher.interpolatePosition(train);

        assertEquals(100, train.getCurrentX());
        assertEquals(100, train.getCurrentY());
    }

    @Test
    void fetchTrainsMock_trainsHaveOriginAndDestination() throws Exception {
        List<Train> trains = trainPositionFetcher.fetchTrainsMock(2);
        for (Train train : trains) {
            assertNotNull(train.getLineOrigin());
            assertNotNull(train.getLineDestination());
        }
    }
}
