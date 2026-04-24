package ch.uzh.ifi.hase.soprafs26.trains;

import ch.uzh.ifi.hase.soprafs26.objects.LineString;
import ch.uzh.ifi.hase.soprafs26.objects.Station;
import ch.uzh.ifi.hase.soprafs26.objects.Train;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

class TrainPositionFetcherTest {

    @MockitoBean
    private TrainPositionFetcher trainPositionFetcher;

    @BeforeEach
    void setUp() {
        trainPositionFetcher = new TrainPositionFetcher();
    }

    @Test
    void fetchTrains_mockModeEnabled_callsFetchTrainsMock() throws Exception {
        TrainPositionFetcher spy = Mockito.spy(new TrainPositionFetcher());

        ReflectionTestUtils.setField(spy, "useMock", true);

            List<Train> mockTrains = List.of(new Train("1"));
            doReturn(mockTrains).when(spy).fetchTrainsMock(1);
            List<Train> result = spy.fetchTrains(1);
            assertEquals(mockTrains, result);

    }

    @Test
    void fetchTrains_liveModeEnabled_callsFetchTrainsLive() throws Exception {
        TrainPositionFetcher spy = Mockito.spy(new TrainPositionFetcher());

        ReflectionTestUtils.setField(spy, "useMock", false);

        List<Train> liveTrains = List.of(new Train("1"));
        doReturn(liveTrains).when(spy).fetchTrainsLive(1);
        List<Train> result = spy.fetchTrains(1);
        assertEquals(liveTrains, result);

    }

    @Test
    void fetchTrainsMock_returnsNonNullList() throws Exception {
        List<Train> result = trainPositionFetcher.fetchTrainsMock(2);
        assertNotNull(result);
    }

    @Test
    void fetchTrainsMock_respectsSubsetSize() throws Exception {
        List<Train> result = trainPositionFetcher.fetchTrainsMock(2);
        assertTrue(result.size() <= 2);
    }

    @Test
    void fetchTrainsMock_returnsEnrichedTrains() throws Exception {
        List<Train> result = trainPositionFetcher.fetchTrainsMock(1);
        assertFalse(result.isEmpty());
        assertNotNull(result.get(0).getTrainId());
    }

    @Test
    void fetchTrainsMock_subsetSizeZero_returnsEmptyList() throws Exception {
        List<Train> result = trainPositionFetcher.fetchTrainsMock(0);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void interpolatePosition_midway_returnsMiddlePoint() {
        Train train = new Train("1");

        Station lastStation = new Station("A", 0, 0, 0, 100);
        Station nextStation = new Station("B", 100, 100, 0, 100);
        train.setLastLeavingStation(lastStation);
        train.setNextPendingStation(nextStation);
        train.setTimestamp(50); // midway

        List<LineString.Point> points = List.of(
                new LineString.Point(0, 0),
                new LineString.Point(100, 100)
        );
        train.setLineString(new LineString(points));

        trainPositionFetcher.interpolatePosition(train);

        assertEquals(50, train.getCurrentX());
        assertEquals(50, train.getCurrentY());
    }

    @Test
    void interpolatePosition_emptyLineString_snapsToLastStation() {
        Train train = new Train("1");

        Station lastStation = new Station("A", 42, 99, 0, 100);
        Station nextStation = new Station("B", 100, 100, 0, 100);
        train.setLastLeavingStation(lastStation);
        train.setNextPendingStation(nextStation);
        train.setTimestamp(50);
        train.setLineString(new LineString(new ArrayList<>()));

        trainPositionFetcher.interpolatePosition(train);

        assertEquals(42, train.getCurrentX());
        assertEquals(99, train.getCurrentY());
    }

    @Test
    void interpolatePosition_timestampBeforeDeparture_snapsToStart() {
        Train train = new Train("1");

        Station lastStation = new Station("A", 0, 0, 100, 200);
        Station nextStation = new Station("B", 100, 100, 100, 200);
        train.setLastLeavingStation(lastStation);
        train.setNextPendingStation(nextStation);
        train.setTimestamp(50); // before departureTime

        List<LineString.Point> points = List.of(
                new LineString.Point(0, 0),
                new LineString.Point(100, 100)
        );
        train.setLineString(new LineString(points));

        trainPositionFetcher.interpolatePosition(train);

        assertEquals(0, train.getCurrentX());
        assertEquals(0, train.getCurrentY());
    }

    @Test
    void interpolatePosition_timestampAfterArrival_snapsToEnd() {
        Train train = new Train("1");

        Station lastStation = new Station("A", 0, 0, 0, 100);
        Station nextStation = new Station("B", 100, 100, 0, 100);
        train.setLastLeavingStation(lastStation);
        train.setNextPendingStation(nextStation);
        train.setTimestamp(200); // after arrivalTime

        List<LineString.Point> points = List.of(
                new LineString.Point(0, 0),
                new LineString.Point(100, 100)
        );
        train.setLineString(new LineString(points));

        trainPositionFetcher.interpolatePosition(train);

        assertEquals(100, train.getCurrentX());
        assertEquals(100, train.getCurrentY());
    }
}


//
//package ch.uzh.ifi.hase.soprafs26.trains;
//
//import ch.uzh.ifi.hase.soprafs26.objects.Station;
//import ch.uzh.ifi.hase.soprafs26.objects.Train;
//import ch.uzh.ifi.hase.soprafs26.objects.LineString;
//
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import org.junit.jupiter.api.BeforeEach;
//
//import java.util.List;
//
//
//public class TrainPositionFetcherTest {
//
//    @Autowired
//    private TrainPositionFetcher fetcher;
//
//    private static final int SUBSET_SIZE = 5;
//
//    @BeforeEach
//    void setUp() {
//        fetcher = new TrainPositionFetcher();
//    }
//
//    @Test
//    public void testFetchTrainsAndPrintResults() throws Exception {
//        System.out.println("=".repeat(70));
//        System.out.println(" FETCHING " + SUBSET_SIZE + " ACTIVE TRAINS FROM SWITZERLAND");
//        System.out.println("=".repeat(70));
//
//        List<Train> trains = fetcher.fetchTrainsMock(SUBSET_SIZE);
//
//        System.out.printf("%nTotal trains returned: %d%n%n", trains.size());
//
//        if (trains.isEmpty()) {
//            System.out.println("No trains returned. Check your API key and network connection.");
//            return;
//        }
//        for (Train t : trains) {
//            fetcher.interpolatePosition(t);
//        }
//
//        for (int i = 0; i < trains.size(); i++) {
//            Train t = trains.get(i);
//            System.out.println("-".repeat(70));
//            System.out.printf("TRAIN %d of %d%n", i + 1, trains.size());
//            System.out.println("-".repeat(70));
//            System.out.printf("  Train ID   : %s%n", t.getTrainId());
//            System.out.printf("  Line       : %s%n", t.getLine() != null ? t.getLine().getName() : "N/A");
//            System.out.println("    Timestamp  : " + (t.getTimestamp() != 0 ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(new java.util.Date(t.getTimestamp())) : "N/A"));
//            System.out.printf("  Origin     : %s%n", nvl(t.getLineOrigin()));
//            System.out.printf("  Destination: %s%n", nvl(t.getLineDestination()));
//            System.out.printf("  Current Position: X=%d, Y=%d%n", t.getCurrentX(), t.getCurrentY());
//
//            //System.out.println("LineString (if available): " + (t.getLineString() != null ? printLineString(t.getLineString()) : "N/A"));
//            printLineString(t.getLineString());
//
//            System.out.println();
//            System.out.println("  >> Last Leaving Station:");
//            printStation(t.getLastLeavingStation());
//
//            System.out.println();
//            System.out.println("  >> Next Pending Station:");
//            printStation(t.getNextPendingStation());
//
//            System.out.println();
//        }
//
//        System.out.println("=".repeat(70));
//        System.out.println(" DONE");
//        System.out.println("=".repeat(70));
//    }
//
//    private void printLineString(LineString ls) {
//        if (ls == null || ls.getPoints() == null) {
//            System.out.println("     (not available)");
//            return;}
//        System.out.println("     LineString Coordinates:");
//        for (int i = 0; i < ls.getPoints().size(); i++) {
//            long coordx = ls.getPoints().get(i).getX();
//            long coordy = ls.getPoints().get(i).getY();
//
//            System.out.printf("       [%d] X: %d, Y: %d%n", i + 1, coordx, coordy);
//        }
//        return;
//    }
//
//    private void printStation(Station s) {
//        if (s == null) {
//            System.out.println("     (not available)");
//            return;
//        }
//        System.out.printf("     Name         : %s%n", nvl(s.getStationName()));
//        System.out.printf("     X (EPSG:3857): %d%n", s.getXCoordinate());
//        System.out.printf("     Y (EPSG:3857): %d%n", s.getYCoordinate());
//        System.out.printf("     Arrival    : %s (epoch ms: %d)%n",
//                formatEpoch(s.getArrivalTime()), s.getArrivalTime());
//        System.out.printf("     Departure    : %s (epoch ms: %d)%n",
//                formatEpoch(s.getDepartureTime()), s.getDepartureTime());
//    }
//
//    private String nvl(String value) {
//        return value != null ? value : "N/A";
//    }
//
//    private String formatEpoch(long epochMs) {
//        if (epochMs == 0) return "N/A";
//        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")
//                .format(new java.util.Date(epochMs));
//    }
//}