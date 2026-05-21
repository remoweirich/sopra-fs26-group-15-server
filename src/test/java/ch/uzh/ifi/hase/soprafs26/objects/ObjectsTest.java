package ch.uzh.ifi.hase.soprafs26.objects;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ObjectsTest {

    // ── Station ──────────────────────────────────────────────────────────

    @Test
    void station_constructorAndGetters() {
        Station s = new Station("Zürich HB", 100L, 200L, 1000L, 2000L);
        assertEquals("Zürich HB", s.getStationName());
        assertEquals(100L, s.getXCoordinate());
        assertEquals(200L, s.getYCoordinate());
        assertEquals(1000L, s.getDepartureTime());
        assertEquals(2000L, s.getArrivalTime());
    }

    @Test
    void station_setters() {
        Station s = new Station();
        s.setStationName("Bern");
        s.setXCoordinate(300L);
        s.setYCoordinate(400L);
        s.setDepartureTime(5000L);
        s.setArrivalTime(6000L);
        assertEquals("Bern", s.getStationName());
        assertEquals(300L, s.getXCoordinate());
    }

    @Test
    void station_toString() {
        Station s = new Station("Basel", 10L, 20L, 0L, 0L);
        assertTrue(s.toString().contains("Basel"));
    }

    // ── LineString ────────────────────────────────────────────────────────

    @Test
    void lineString_constructorAndGetters() {
        LineString.Point p = new LineString.Point(1L, 2L);
        LineString ls = new LineString(List.of(p));
        assertEquals(1, ls.getPoints().size());
        assertEquals(1L, ls.getPoints().get(0).getX());
        assertEquals(2L, ls.getPoints().get(0).getY());
    }

    @Test
    void lineString_point_setters() {
        LineString.Point p = new LineString.Point();
        p.setX(5L);
        p.setY(10L);
        assertEquals(5L, p.getX());
        assertEquals(10L, p.getY());
        assertTrue(p.toString().contains("5"));
    }

    @Test
    void lineString_setPoints() {
        LineString ls = new LineString();
        ls.setPoints(List.of(new LineString.Point(3L, 4L)));
        assertEquals(1, ls.getPoints().size());
    }

    // ── Train ─────────────────────────────────────────────────────────────

    @Test
    void train_constructorAndGetters() {
        Train t = new Train("IC1");
        assertEquals("IC1", t.getTrainId());
    }

    @Test
    void train_copyConstructor() {
        Train original = new Train("RE2");
        original.setCurrentX(100L);
        original.setCurrentY(200L);
        Train.Line line = new Train.Line("S1");
        original.setLine(line);

        Train copy = new Train(original);
        assertEquals("RE2", copy.getTrainId());
        assertEquals(100L, copy.getCurrentX());
        assertEquals(200L, copy.getCurrentY());
        assertEquals("S1", copy.getLine().getName());
    }

    @Test
    void train_setters() {
        Train t = new Train();
        t.setTrainId("IR15");
        t.setCurrentX(50L);
        t.setCurrentY(60L);
        t.setDepartureTime(1000L);
        t.setArrivalTime(2000L);
        t.setTimestamp(3000L);
        assertEquals("IR15", t.getTrainId());
        assertEquals(50L, t.getCurrentX());
        assertEquals(1000L, t.getDepartureTime());
        assertEquals(3000L, t.getTimestamp());
    }

    @Test
    void train_toString_withLineString() {
        Train t = new Train("IC8");
        LineString ls = new LineString(List.of(new LineString.Point(1L, 2L)));
        t.setLineString(ls);
        assertTrue(t.toString().contains("IC8"));
        assertTrue(t.toString().contains("1"));
    }

    @Test
    void train_line_constructorAndGetters() {
        Train.Line line = new Train.Line("S3");
        assertEquals("S3", line.getName());
        assertTrue(line.toString().contains("S3"));
    }

    @Test
    void train_line_setter() {
        Train.Line line = new Train.Line();
        line.setName("IC2000");
        assertEquals("IC2000", line.getName());
    }

    // ── UserGameStatus ────────────────────────────────────────────────────

    @Test
    void userGameStatus_constructorAndGetters() {
        UserGameStatus s = new UserGameStatus(1L, true);
        assertEquals(1L, s.getUserId());
        assertTrue(s.getIsReady());
    }

    // ── UserResult ────────────────────────────────────────────────────────

    @Test
    void userResult_constructorAndGetters() {
        UserResult r = new UserResult(1L, 500, 200, 100L, 200L, 5.0);
        assertEquals(1L, r.getUserId());
        assertEquals(500, r.getTotalPoints());
        assertEquals(200, r.getRoundPoints());
        assertEquals(5.0, r.getDistance());
    }
}