package ch.uzh.ifi.hase.soprafs26.trains;

import ch.uzh.ifi.hase.soprafs26.objects.Station;
import ch.uzh.ifi.hase.soprafs26.objects.Train;
import ch.uzh.ifi.hase.soprafs26.objects.LineString;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.core.io.ClassPathResource;

import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

/**
 * Fetches real-time train positions from the geOps Realtime WebSocket API.
 *
 * When USE_MOCK = true, no network calls are made. Instead, messages are read
 * from src/main/resources/mock_train_messages.json, which must contain:
 *   {
 *     "trajectoryMessages": [ ...PartialTrajectoryMessage objects... ],
 *     "stopSequenceMessages": [ ...StopSequenceMessage objects... ]
 *   }
 *
 * Toggle via application.properties:
 *   geops.mock=true   → use cached mock data
 *   geops.mock=false  → connect to live API (default)
 *
 * Protocol overview (AsyncAPI spec):
 *   1. Connect to wss://api.geops.io/tracker-ws/v1/?key=API_KEY
 *   2. Wait for {"source":"websocket","content":{"status":"open"}}
 *   3. Send: BBOX <left> <bottom> <right> <top> <zoom> [filters]
 *      → server streams PartialTrajectoryMessages (source="trajectory")
 *      → each message contains content.geometry.coordinates (the LineString)
 *        and content.properties (train metadata incl. train_id and line)
 *   4. After collecting enough trains, send: GET stopsequence_<train_id>
 *      → server replies with StopSequenceMessage (source="stopsequence_<train_id>")
 *
 * Switzerland bounding box (EPSG:3857):
 *   left=640000  bottom=5730000  right=1200000  top=6100000  zoom=7
 */
@Service
public class TrainPositionFetcher {

    private static final Logger log = LoggerFactory.getLogger(TrainPositionFetcher.class);

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    /** Toggle via application.properties: geops.mock=true */
    @Value("${geops.mock:false}")
    private boolean useMock;

    @Value("${geops.api.key}")
    private String apiKey;


    private static final String SWITZERLAND_BBOX =
            "BBOX 640000 5730000 1200000 6100000 7 mots=rail";

    private static final long BBOX_COLLECT_TIMEOUT_MS  = 6_000; //prob need to set it higher
    private static final long STOP_SEQUENCE_TIMEOUT_MS = 5_000;
    private static final String WS_BASE_URL = "wss://api.geops.io/tracker-ws/v1/";
    private static final String MOCK_FILE   = "mock_train_messages.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns a list of enriched Train objects. Uses mock data if geops.mock=true.
     *
     * @param subsetSize number of trains to return
     */
    public List<Train> fetchTrains(int subsetSize) throws Exception {
        if (useMock) {
            log.info("[MOCK] USE_MOCK=true — loading cached messages from {}", MOCK_FILE);
            return fetchTrainsMock(subsetSize);
        }
        log.info("USE_MOCK=false — connecting to live WebSocket API");
        return fetchTrainsLive(subsetSize);
    }

    // -------------------------------------------------------------------------
    // Mock path
    // -------------------------------------------------------------------------

    public List<Train> fetchTrainsMock(int subsetSize) throws Exception {
        JsonNode root = objectMapper.readTree(
                new ClassPathResource(MOCK_FILE).getInputStream());

        // --- Phase 1: parse trajectory messages → discover trains + LineStrings ---
        Map<String, Train> discoveredTrains = new LinkedHashMap<>();

        for (JsonNode msg : root.path("trajectoryMessages")) {
            JsonNode content = msg.path("content");
            if (content.isNull() || content.isMissingNode()) continue;

            JsonNode props   = content.path("properties");
            String   trainId = props.path("train_id").asText(null);
            long   timestamp = props.path("timestamp").asLong();

            if (trainId == null) continue;

            Train train = discoveredTrains.computeIfAbsent(trainId, id -> {
                Train t = new Train(id);
                t.setTimestamp(timestamp);
                JsonNode lineNode = props.path("line");
                if (!lineNode.isMissingNode() && !lineNode.isNull()) {
                    t.setLine(new Train.Line(lineNode.path("name").asText(null)));

                }
                log.debug("[MOCK] Discovered train: {}", id);
                return t;
            });

            // Parse LineString from content.geometry.coordinates
            // (overwrite on duplicate — last message wins, matching live behaviour)
            LineString lineString = parseLineString(content.path("geometry").path("coordinates"));
            if (lineString != null) {
                train.setLineString(lineString);
            }
        }

        log.info("[MOCK] Discovered {} trains from mock trajectory messages", discoveredTrains.size());

        // --- Phase 2: pick random subset ---
        List<String> allIds = new ArrayList<>(discoveredTrains.keySet());
        Collections.shuffle(allIds);
        List<String> selectedIds = allIds.subList(0, Math.min(subsetSize, allIds.size()));

        // --- Phase 3: index stop-sequence messages by train id ---
        Map<String, JsonNode> stopSequenceData = new HashMap<>();
        for (JsonNode msg : root.path("stopSequenceMessages")) {
            String source = msg.path("source").asText("");
            if (source.startsWith("stopsequence_")) {
                String trainId = source.substring("stopsequence_".length());
                stopSequenceData.put(trainId, msg.path("content"));
            }
        }

        // --- Phase 4: enrich and return ---
        List<Train> result = new ArrayList<>();
        for (String trainId : selectedIds) {
            Train    train      = discoveredTrains.get(trainId);
            JsonNode seqContent = stopSequenceData.get(trainId);

            if (seqContent == null || seqContent.isNull()) {
                log.warn("[MOCK] No stop sequence data for train {}, skipping", trainId);
                continue;
            }

            enrichTrainFromStopSequence(train, seqContent);
            result.add(train);
        }

        log.info("[MOCK] Returning {} enriched trains", result.size());
        return result;
    }

    // -------------------------------------------------------------------------
    // Live WebSocket path
    // -------------------------------------------------------------------------

    public List<Train> fetchTrainsLive(int subsetSize) throws Exception {
        Map<String, Train>          discoveredTrains = new ConcurrentHashMap<>();
        Map<String, CountDownLatch> ssLatches        = new ConcurrentHashMap<>();
        Map<String, JsonNode>       stopSequenceData = new ConcurrentHashMap<>();

        CountDownLatch openLatch = new CountDownLatch(1);
        CountDownLatch bboxDone  = new CountDownLatch(1);

        // -----------------------------------------------------------------
        // WebSocket handler
        // -----------------------------------------------------------------
        WebSocketHandler handler = new AbstractWebSocketHandler() {

            @Override
            public void afterConnectionEstablished(WebSocketSession s) {
                log.debug("WebSocket connected");
            }

            @Override
            protected void handleTextMessage(WebSocketSession s, TextMessage message) {
                try {
                    JsonNode root   = objectMapper.readTree(message.getPayload());
                    String   source = root.path("source").asText("");

                    // Connection ready
                    if ("websocket".equals(source)) {
                        if ("open".equals(root.path("content").path("status").asText())) {
                            log.debug("WebSocket open, sending BBOX");
                            openLatch.countDown();
                            s.sendMessage(new TextMessage(SWITZERLAND_BBOX));
                        }
                        return;
                    }

                    // Partial trajectory — extract metadata AND LineString geometry
                    if ("trajectory".equals(source)) {
                        if (bboxDone.getCount() == 0) return;
                        JsonNode content = root.path("content");
                        if (content.isNull() || content.isMissingNode()) return;

                        JsonNode props   = content.path("properties");
                        String   trainId = props.path("train_id").asText(null);
                        
                        long   timestamp = props.path("timestamp").asLong();

                        if (trainId == null) return;

                        Train train = discoveredTrains.computeIfAbsent(trainId, id -> {
                            Train t = new Train(id);
                            t.setTimestamp(timestamp);
                            JsonNode lineNode = props.path("line");
                            if (!lineNode.isMissingNode() && !lineNode.isNull()) {
                                t.setLine(new Train.Line(lineNode.path("name").asText(null)));
                            }
                            log.debug("Discovered train: {}", id);
                            return t;
                        });

                        // Parse LineString from content.geometry.coordinates
                        // (overwrite on duplicate — last message wins, i.e. most recent segment)
                        LineString lineString = parseLineString(
                                content.path("geometry").path("coordinates"));
                        if (lineString != null) {
                            train.setLineString(lineString);
                        }
                        return;
                    }

                    // Stop sequence response
                    if (source.startsWith("stopsequence_")) {
                        String trainId = source.substring("stopsequence_".length());
                        stopSequenceData.put(trainId, root.path("content"));
                        CountDownLatch latch = ssLatches.get(trainId);
                        if (latch != null) latch.countDown();
                    }

                } catch (Exception e) {
                    log.warn("Error processing WebSocket message: {}", e.getMessage());
                }
            }

            @Override
            public void handleTransportError(WebSocketSession s, Throwable ex) {
                log.error("WebSocket transport error: {}", ex.getMessage());
                openLatch.countDown();
                bboxDone.countDown();
            }

            @Override
            public void afterConnectionClosed(WebSocketSession s, CloseStatus status) {
                log.debug("WebSocket closed: {}", status);
                bboxDone.countDown();
            }
        };

        // -----------------------------------------------------------------
        // Connect
        // -----------------------------------------------------------------
        WebSocketClient client = new StandardWebSocketClient();
        String url = WS_BASE_URL + "?key=" + apiKey;
        WebSocketSession session = client
                .execute(handler, new WebSocketHttpHeaders(), URI.create(url))
                .get(10, TimeUnit.SECONDS);

        // -----------------------------------------------------------------
        // Phase 1: collect BBOX vehicles
        // -----------------------------------------------------------------
        if (!openLatch.await(10, TimeUnit.SECONDS)) {
            session.close();
            throw new IllegalStateException("WebSocket did not open in time");
        }
        Thread.sleep(BBOX_COLLECT_TIMEOUT_MS);
        bboxDone.countDown();

        log.info("Discovered {} active trains", discoveredTrains.size());

        if (discoveredTrains.isEmpty()) {
            session.close();
            return Collections.emptyList();
        }

        // -----------------------------------------------------------------
        // Phase 2: random subset
        // -----------------------------------------------------------------
        List<String> allIds = new ArrayList<>(discoveredTrains.keySet());
        Collections.shuffle(allIds);
        List<String> selectedIds = allIds.subList(0, Math.min(subsetSize, allIds.size()));
        log.info("Selected {} trains for stop-sequence enrichment", selectedIds.size());

        // -----------------------------------------------------------------
        // Phase 3: request stop sequences
        // -----------------------------------------------------------------
        for (String trainId : selectedIds) {
            ssLatches.put(trainId, new CountDownLatch(1));
            session.sendMessage(new TextMessage("GET stopsequence_" + trainId));
        }
        for (String trainId : selectedIds) {
            if (!ssLatches.get(trainId).await(STOP_SEQUENCE_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                log.warn("Timeout waiting for stop sequence of {}", trainId);
            }
        }
        session.close();

        // -----------------------------------------------------------------
        // Phase 4: enrich and return
        // -----------------------------------------------------------------
        List<Train> result = new ArrayList<>();
        for (String trainId : selectedIds) {
            Train    train      = discoveredTrains.get(trainId);
            JsonNode seqContent = stopSequenceData.get(trainId);
            if (seqContent == null || seqContent.isNull()) {
                log.warn("No stop sequence data for train {}, skipping", trainId);
                continue;
            }
            enrichTrainFromStopSequence(train, seqContent);
            result.add(train);
        }

        log.info("Returning {} enriched trains", result.size());
        return result;
    }

    // -------------------------------------------------------------------------
    // Interpolating current train position from enriched Train object
    // -------------------------------------------------------------------------

    public void interpolatePosition(Train train) {
        Station lastStation = train.getLastLeavingStation();
        Station nextStation = train.getNextPendingStation();
        LineString lineString = train.getLineString();

        long departureTime = lastStation.getDepartureTime();
        long arrivalTime = nextStation.getArrivalTime();
        long currentTime = train.getTimestamp();

        // Clamp current time to the valid range
        currentTime = Math.max(departureTime, Math.min(currentTime, arrivalTime));

        // Total travel time and elapsed time
        double totalTime = arrivalTime - departureTime;
        double elapsedTime = currentTime - departureTime;

        // Overall progress ratio [0.0, 1.0]
        double progressRatio = (totalTime == 0) ? 1.0 : elapsedTime / totalTime;

        List<LineString.Point> points = lineString.getPoints();

        if (points == null || points.isEmpty()) {
            train.setCurrentX(lastStation.getXCoordinate());
            train.setCurrentY(lastStation.getYCoordinate());
            return;
        }

        // Calculate total path length
        double totalLength = 0.0;
        double[] segmentLengths = new double[points.size() - 1];
        for (int i = 0; i < points.size() - 1; i++) {
            segmentLengths[i] = distance(points.get(i), points.get(i + 1));
            totalLength += segmentLengths[i];
        }

        // Target distance along the path
        double targetDistance = progressRatio * totalLength;

        // Walk along segments to find the interpolated point
        double accumulated = 0.0;
        for (int i = 0; i < segmentLengths.length; i++) {
            double segLen = segmentLengths[i];
            if (accumulated + segLen >= targetDistance || i == segmentLengths.length - 1) {
                // The position lies within this segment
                double segmentProgress = (segLen == 0) ? 0.0 : (targetDistance - accumulated) / segLen;
                segmentProgress = Math.max(0.0, Math.min(1.0, segmentProgress));

                LineString.Point p1 = points.get(i);
                LineString.Point p2 = points.get(i + 1);

                long interpolatedX = Math.round(p1.getX() + segmentProgress * (p2.getX() - p1.getX()));
                long interpolatedY = Math.round(p1.getY() + segmentProgress * (p2.getY() - p1.getY()));

                train.setCurrentX(interpolatedX);
                train.setCurrentY(interpolatedY);
                return;
            }
            accumulated += segLen;
        }

        // Fallback: snap to the last point
        LineString.Point last = points.get(points.size() - 1);
        train.setCurrentX(last.getX());
        train.setCurrentY(last.getY());
    }

    private double distance(LineString.Point a, LineString.Point b) {
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    // -------------------------------------------------------------------------
    // Shared parsing helpers
    // -------------------------------------------------------------------------

    /**
     * Parses content.geometry.coordinates from a PartialTrajectoryMessage into a LineString.
     *
     * The coordinates node is a JSON array of [x, y] pairs in EPSG:3857, e.g.:
     *   [[951560, 6003400], [952100, 6003800], ...]
     *
     * Returns null if the node is missing, null, or contains no valid points.
     */
    private LineString parseLineString(JsonNode coordinatesNode) {
        if (coordinatesNode == null || coordinatesNode.isNull() || coordinatesNode.isMissingNode()) {
            return null;
        }
        if (!coordinatesNode.isArray() || coordinatesNode.size() == 0) {
            return null;
        }

        List<LineString.Point> points = new ArrayList<>();
        for (JsonNode coord : coordinatesNode) {
            if (coord.isArray() && coord.size() >= 2) {
                long x = coord.get(0).asLong();
                long y = coord.get(1).asLong();
                points.add(new LineString.Point(x, y));
            }
        }

        return points.isEmpty() ? null : new LineString(points);
    }

    /**
     * Parses a StopSequenceMessage content node and enriches the Train.
     *
     * Station states:
     *   LEAVING  → train has already departed this stop
     *   BOARDING → train is currently at this stop (treated as last leaving)
     *   PENDING  → train has not yet arrived
     */
    private void enrichTrainFromStopSequence(Train train, JsonNode content) {
        if (!content.isArray() || content.size() == 0) {
            log.warn("Empty stop sequence for train {}", train.getTrainId());
            return;
        }

        JsonNode seq = content.get(0);
        //train.setLineDestination(seq.path("destination").asText(null));


        JsonNode stations = seq.path("stations");
        if (!stations.isArray() || stations.size() == 0) return;

        Station lineOrigin = parseStation(stations.get(0));
        Station lineDestination = parseStation(stations.get(stations.size() - 1));
        //train.setLineOrigin(stations.get(0).path("stationName").asText(null));
        train.setLineOrigin(parseStation(stations.get(0)));

        train.setLineDestination(parseStation(stations.get(stations.size() - 1)));

        //set the departure and arrival time for the origin and final destination stations

        train.setDepartureTime(lineOrigin.getDepartureTime());
        train.setArrivalTime(lineDestination.getArrivalTime());


        Station lastLeaving = null;
        Station nextPending = null;

        for (JsonNode stationNode : stations) {
            String state = stationNode.path("state").asText("");
            if ("LEAVING".equals(state) || "BOARDING".equals(state)) {
                lastLeaving = parseStation(stationNode);
            } else if ("PENDING".equals(state) && nextPending == null) {
                nextPending = parseStation(stationNode);
                break;
            }
        }

        train.setLastLeavingStation(lastLeaving);
        train.setNextPendingStation(nextPending);

        log.debug("Train {} enriched: {} → {} | between '{}' and '{}' | lineString points: {}",
                train.getTrainId(),
                train.getLineOrigin(),
                train.getLineDestination(),
                lastLeaving != null ? lastLeaving.getStationName() : "?",
                nextPending != null ? nextPending.getStationName() : "?",
                train.getLineString() != null ? train.getLineString().getPoints().size() : 0);
    }

    /** Converts a StopSequenceCall JSON node into a Station. coordinate = [x, y] in EPSG:3857. */
    private Station parseStation(JsonNode node) {
        String name = node.path("stationName").asText(null);
        long x = 0, y = 0;
        JsonNode coord = node.path("coordinate");
        if (coord.isArray() && coord.size() >= 2) {
            x = coord.get(0).asLong();
            y = coord.get(1).asLong();
        }
        long departureTime = node.path("departureTime").asLong(0);
        long arrivalTime = node.path("arrivalTime").asLong(0);

        return new Station(name, x, y, departureTime, arrivalTime);
    }
}