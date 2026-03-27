/*package ch.uzh.ifi.hase.soprafs26.trains;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import ch.uzh.ifi.hase.soprafs26.trains.model.*;



@Service
@ConditionalOnProperty(prefix = "gtfs", name = "static-url")
@Slf4j
public class GtfsStaticLoader {

    @Value("${gtfs.static-url:}") private String staticUrl;
    @Value("${gtfs.api-token:}")  private String apiToken;
    @Value("${gtfs.local-zip-path:}") private String localZipPath;

    private final GtfsDataStore store;

    public GtfsStaticLoader(GtfsDataStore store) { this.store = store; }

    // Run once on startup, then on schedule
    @PostConstruct
    public void loadOnStartup() {
        CompletableFuture.runAsync(this::downloadAndParse);
    }

    @Scheduled(cron = "${gtfs.reload-cron}")
    public void scheduledReload() {
        log.info("Scheduled GTFS static reload starting...");
        downloadAndParse();
    }

    public void downloadAndParse() {
        try {
            Path zipPath = Path.of(localZipPath);
            Files.createDirectories(zipPath.getParent());

            log.info("Downloading GTFS static ZIP..." + (staticUrl != null ? "from " + staticUrl : "to local path " + localZipPath));
            System.out.println("Downloading GTFS static ZIP..." + (staticUrl != null ? "from " + staticUrl : "to local path " + localZipPath));

            downloadZip(zipPath);

            log.info("Parsing GTFS static data (this may take a minute)...");
            System.out.println("Parsing GTFS static data (this may take a minute)...");

            parseAndLoad(zipPath);

            log.info("GTFS static loaded at {}", store.loadedAt());
            System.out.println("GTFS static loaded at {}" + store.loadedAt().toString());

        } catch (Exception e) {
            log.error("Failed to load GTFS static data", e);
        }
    }

    private void downloadZip(Path dest) throws Exception {
        // The dataset page redirects to the actual file download
        // You can also hardcode the direct resource URL if you know it
        HttpURLConnection conn = (HttpURLConnection)
            new URL(staticUrl).openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("Authorization", "Bearer " + apiToken);
        conn.setRequestProperty("User-Agent", "SwissTrainTracker/1.0");

        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("ZIP downloaded: {} MB", Files.size(dest) / 1_000_000);
    }

    public void parseAndLoad(Path zipPath) throws Exception {
        Map<String, Stop>            stops     = new HashMap<>(50_000);
        Map<String, List<StopTime>>  stopTimes = new HashMap<>(100_000);
        Map<String, String>          tripShapes = new HashMap<>(100_000);
        Map<String, String>          tripNames  = new HashMap<>(100_000);
        Map<String, List<ShapePoint>> shapes   = new HashMap<>(10_000);

        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            parseStops(zip, stops);
            parseTrips(zip, tripShapes, tripNames);
            parseStopTimes(zip, stopTimes);
            parseShapes(zip, shapes);
        }

        // Sort stop sequences
        stopTimes.values().forEach(list ->
            list.sort(Comparator.comparingInt(StopTime::stopSeq)));
        shapes.values().forEach(list ->
            list.sort(Comparator.comparingInt(ShapePoint::seq)));

        store.update(stops, stopTimes, tripShapes, tripNames, shapes);
    }

    // ── Parsing helpers (stream line-by-line to avoid OOM) ───────────────────

    private void parseStops(ZipFile zip, Map<String, Stop> out) throws Exception {
        try (BufferedReader r = entryReader(zip, "stops.txt")) {
            String[] h = r.readLine().split(",");
            int idI = col(h, "stop_id"), latI = col(h, "stop_lat"),
                lonI = col(h, "stop_lon");
            String line;
            while ((line = r.readLine()) != null) {
                String[] f = splitLine(line);
                out.put(strip(f[idI]), new Stop(strip(f[idI]),
                    Double.parseDouble(strip(f[latI])),
                    Double.parseDouble(strip(f[lonI]))));
            }
        }
    }

    private void parseTrips(ZipFile zip, Map<String, String> shapes,
                            Map<String, String> names) throws Exception {
        try (BufferedReader r = entryReader(zip, "trips.txt")) {
            String[] h = r.readLine().split(",");
            int tripI = col(h, "trip_id"), nameI = col(h, "trip_short_name"),
                shpI  = colOpt(h, "shape_id");
            String line;
            while ((line = r.readLine()) != null) {
                String[] f = splitLine(line);
                String tid = strip(f[tripI]);
                names.put(tid, strip(f[nameI]));
                if (shpI >= 0 && !f[shpI].isBlank())
                    shapes.put(tid, strip(f[shpI]));
            }
        }
    }

    private void parseStopTimes(ZipFile zip,
                                Map<String, List<StopTime>> out) throws Exception {
        try (BufferedReader r = entryReader(zip, "stop_times.txt")) {
            String[] h = r.readLine().split(",");
            int tripI = col(h, "trip_id"), stopI = col(h, "stop_id"),
                seqI  = col(h, "stop_sequence"),
                arrI  = col(h, "arrival_time"), depI = col(h, "departure_time");
            String line;
            while ((line = r.readLine()) != null) {
                String[] f = splitLine(line);
                String tid = strip(f[tripI]);
                out.computeIfAbsent(tid, k -> new ArrayList<>())
                   .add(new StopTime(tid, strip(f[stopI]),
                       Integer.parseInt(strip(f[seqI])),
                       parseHms(strip(f[arrI])), parseHms(strip(f[depI]))));
            }
        }
    }

    private void parseShapes(ZipFile zip,
                             Map<String, List<ShapePoint>> out) throws Exception {
        ZipEntry e = zip.getEntry("shapes.txt");
        if (e == null) { log.warn("No shapes.txt in ZIP"); return; }
        try (BufferedReader r = entryReader(zip, "shapes.txt")) {
            String[] h = r.readLine().split(",");
            int shpI = col(h, "shape_id"), latI = col(h, "shape_pt_lat"),
                lonI = col(h, "shape_pt_lon"), seqI = col(h, "shape_pt_sequence"),
                dstI = colOpt(h, "shape_dist_traveled");
            String line; int autoSeq = 0;
            while ((line = r.readLine()) != null) {
                String[] f = splitLine(line);
                double dist = (dstI >= 0 && !strip(f[dstI]).isEmpty())
                    ? Double.parseDouble(strip(f[dstI])) : autoSeq++;
                out.computeIfAbsent(strip(f[shpI]), k -> new ArrayList<>())
                   .add(new ShapePoint(strip(f[shpI]),
                       Double.parseDouble(strip(f[latI])),
                       Double.parseDouble(strip(f[lonI])),
                       Integer.parseInt(strip(f[seqI])), dist));
            }
        }
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private static BufferedReader entryReader(ZipFile zip, String name)
            throws Exception {
        return new BufferedReader(
            new InputStreamReader(zip.getInputStream(zip.getEntry(name)), "UTF-8"),
            1 << 16);  // 64 KB buffer for large files
    }

    private static int parseHms(String s) {
        if (s == null || s.isBlank()) return 0;
        String[] p = s.split(":");
        return Integer.parseInt(p[0]) * 3600 + Integer.parseInt(p[1]) * 60
             + Integer.parseInt(p[2]);
    }

    private static String strip(String s) {
        return s == null ? "" : s.trim().replace("\"", "");
    }

    private static String[] splitLine(String line) {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }

    private static int col(String[] h, String name) {
        for (int i = 0; i < h.length; i++)
            if (h[i].trim().replace("\"","").equalsIgnoreCase(name)) return i;
        throw new IllegalArgumentException("Column not found: " + name);
    }
    private static int colOpt(String[] h, String name) {
        for (int i = 0; i < h.length; i++)
            if (h[i].trim().replace("\"","").equalsIgnoreCase(name)) return i;
        return -1;
    }
}
    */
package ch.uzh.ifi.hase.soprafs26.trains;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import ch.uzh.ifi.hase.soprafs26.trains.model.*;


@Service
@ConditionalOnProperty(prefix = "gtfs", name = "static-url")
@Slf4j
public class GtfsStaticLoader {

    @Value("${gtfs.static-url:}")      private String staticUrl;
    @Value("${gtfs.api-token:}")       private String apiToken;
    @Value("${gtfs.local-zip-path:}")  private String localZipPath;

    private final GtfsDataStore store;

    public GtfsStaticLoader(GtfsDataStore store) { this.store = store; }

    @PostConstruct
    public void loadOnStartup() {
        CompletableFuture.runAsync(this::downloadAndParse);
    }

    @Scheduled(cron = "${gtfs.reload-cron}")
    public void scheduledReload() {
        log.info("Scheduled GTFS static reload starting...");
        downloadAndParse();
    }

    public void downloadAndParse() {
        try {
            Path zipPath = Path.of(localZipPath);
            Files.createDirectories(zipPath.getParent());

            log.info("Downloading GTFS static ZIP from {}", staticUrl);
            downloadZip(zipPath);

            log.info("Parsing GTFS static data (this may take a minute)...");
            parseAndLoad(zipPath);

            log.info("GTFS static loaded at {}", store.loadedAt());
        } catch (Exception e) {
            log.error("Failed to load GTFS static data", e);
        }
    }

    private void downloadZip(Path dest) throws Exception {
        HttpURLConnection conn = (HttpURLConnection)
            new URL(staticUrl).openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("Authorization", "Bearer " + apiToken);
        conn.setRequestProperty("User-Agent", "SwissTrainTracker/1.0");
        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("ZIP downloaded: {} MB", Files.size(dest) / 1_000_000);
    }

    public void parseAndLoad(Path zipPath) throws Exception {
        Map<String, Stop>             stops      = new HashMap<>(50_000);
        Map<String, List<StopTime>>   stopTimes  = new HashMap<>(100_000);
        Map<String, String>           tripShapes = new HashMap<>(100_000);
        Map<String, String>           tripNames  = new HashMap<>(100_000);
        Map<String, List<ShapePoint>> shapes     = new HashMap<>(10_000);

        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            parseStops(zip, stops);
            parseTrips(zip, tripShapes, tripNames);
            parseStopTimes(zip, stopTimes);
            parseShapes(zip, shapes);   // no-op if shapes.txt absent (Swiss feed)
        }

        stopTimes.values().forEach(list ->
            list.sort(Comparator.comparingInt(StopTime::stopSeq)));
        shapes.values().forEach(list ->
            list.sort(Comparator.comparingInt(ShapePoint::seq)));

        store.update(stops, stopTimes, tripShapes, tripNames, shapes);
        System.out.println("GTFS static trips, stops, stoptimes successfully loaded at " + store.loadedAt());
    }

    // ── Parsers ──────────────────────────────────────────────────────────────

    /**
     * stops.txt columns (Swiss GTFS profile):
     *   stop_id, stop_name, stop_lat, stop_lon,
     *   location_type, parent_station, platform_code, original_stop_id
     *
     * Notes:
     * - stop_id can contain colons, e.g. "8507000:0:49"
     * - stop_name may contain commas → must use quoted-CSV split
     * - location_type is empty for platforms, "1" for parent stations
     * - original_stop_id holds the Swiss Location ID (SLOID) when available
     */
    private void parseStops(ZipFile zip, Map<String, Stop> out) throws Exception {
        try (BufferedReader r = entryReader(zip, "stops.txt")) {
            String[] h = csvSplitLine(r.readLine());
            int idI   = col(h, "stop_id");
            int nameI = col(h, "stop_name");
            int latI  = col(h, "stop_lat");
            int lonI  = col(h, "stop_lon");
            // Optional Swiss-specific columns (present since Oct 2025)
            int platformI = colOpt(h, "platform_code");
            int sloidI    = colOpt(h, "original_stop_id");

            String line;
            while ((line = r.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] f = csvSplitLine(line);
                if (f.length <= lonI) continue;          // guard against short rows

                String stopId    = strip(f[idI]);
                String stopName  = strip(f[nameI]);
                double lat       = parseDouble(strip(f[latI]));
                double lon       = parseDouble(strip(f[lonI]));
                String platform  = (platformI >= 0 && platformI < f.length)
                                   ? strip(f[platformI]) : "";
                String sloid     = (sloidI >= 0 && sloidI < f.length)
                                   ? strip(f[sloidI]) : "";

                out.put(stopId, new Stop(stopId, stopName, lat, lon, platform, sloid));
            }
        }
        log.info("Loaded {} stops", out.size());
    }

    /**
     * trips.txt columns used:
     *   trip_id, trip_short_name (train number for trains), shape_id (optional)
     *
     * trip_id format: <seq>.<service_id>.<route_id>.<path_no>.<direction>
     * e.g. "1.TA.91-10-A-j26-1.1.H"
     */
    private void parseTrips(ZipFile zip,
                            Map<String, String> shapes,
                            Map<String, String> names) throws Exception {
        try (BufferedReader r = entryReader(zip, "trips.txt")) {
            String[] h = csvSplitLine(r.readLine());
            int tripI  = col(h, "trip_id");
            int nameI  = col(h, "trip_short_name");
            int shpI   = colOpt(h, "shape_id");

            String line;
            while ((line = r.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] f = csvSplitLine(line);
                if (f.length <= nameI) continue;

                String tid = strip(f[tripI]);
                names.put(tid, strip(f[nameI]));
                if (shpI >= 0 && shpI < f.length && !strip(f[shpI]).isEmpty()) {
                    shapes.put(tid, strip(f[shpI]));
                }
            }
        }
        log.info("Loaded {} trips", names.size());
    }

    /**
     * stop_times.txt columns:
     *   trip_id, arrival_time, departure_time, stop_id, stop_sequence,
     *   pickup_type, drop_off_type
     *
     * Important: times may exceed 24:00:00 (trips crossing midnight belong
     * to the same operating day, per Swiss GTFS profile).
     */
    private void parseStopTimes(ZipFile zip,
                                Map<String, List<StopTime>> out) throws Exception {
        try (BufferedReader r = entryReader(zip, "stop_times.txt")) {
            String[] h = r.readLine().split(",");
            int tripI = col(h, "trip_id"), stopI = col(h, "stop_id"),
                seqI  = col(h, "stop_sequence"),
                arrI  = col(h, "arrival_time"), depI = col(h, "departure_time");
            String line;
            Set<String> seenTripIds = new HashSet<>();
            while ((line = r.readLine()) != null) {
                String[] f = csvSplitLine(line);
                String tid = strip(f[tripI]);
                
                // Skip if we've already seen 50 unique trip_ids and this is a new one
                if (seenTripIds.size() >= 50 && !seenTripIds.contains(tid)) {
                    continue;
                }
                
                seenTripIds.add(tid);
                out.computeIfAbsent(tid, k -> new ArrayList<>())
                   .add(new StopTime(tid, strip(f[stopI]),
                       Integer.parseInt(strip(f[seqI])),
                       parseHms(strip(f[arrI])), parseHms(strip(f[depI]))));
            }
        }
    }

    /**
     * shapes.txt — explicitly NOT provided in the current Swiss GTFS feed.
     * Method is a no-op when the entry is absent; warns once so the absence
     * is visible in logs without failing startup.
     */
    private void parseShapes(ZipFile zip,
                             Map<String, List<ShapePoint>> out) throws Exception {
        ZipEntry e = zip.getEntry("shapes.txt");
        if (e == null) {
            log.info("shapes.txt not present in ZIP (expected for Swiss GTFS feed — " +
                     "generate with pfaedle if needed)");
            return;
        }
        try (BufferedReader r = entryReader(zip, "shapes.txt")) {
            String[] h = csvSplitLine(r.readLine());
            int shpI = col(h, "shape_id");
            int latI = col(h, "shape_pt_lat");
            int lonI = col(h, "shape_pt_lon");
            int seqI = col(h, "shape_pt_sequence");
            int dstI = colOpt(h, "shape_dist_traveled");

            String line; int autoSeq = 0;
            while ((line = r.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] f = csvSplitLine(line);
                if (f.length <= seqI) continue;

                double dist = (dstI >= 0 && dstI < f.length && !strip(f[dstI]).isEmpty())
                    ? parseDouble(strip(f[dstI])) : autoSeq++;
                out.computeIfAbsent(strip(f[shpI]), k -> new ArrayList<>())
                   .add(new ShapePoint(
                       strip(f[shpI]),
                       parseDouble(strip(f[latI])),
                       parseDouble(strip(f[lonI])),
                       Integer.parseInt(strip(f[seqI])),
                       dist
                   ));
            }
        }
        log.info("Loaded shapes for {} shape IDs", out.size());
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    /** Open a ZIP entry as a buffered UTF-8 reader with a 64 KB buffer. */
    private static BufferedReader entryReader(ZipFile zip, String name)
        throws Exception {
    ZipEntry entry = zip.getEntry(name);
    if (entry == null)
        throw new IllegalArgumentException("Entry not found in ZIP: " + name);

    BufferedReader r = new BufferedReader(
        new InputStreamReader(zip.getInputStream(entry), "UTF-8"),
        1 << 16);

    // Strip UTF-8 BOM (\uFEFF) if present — common in Microsoft-generated CSVs
    r.mark(1);
    if (r.read() != '\uFEFF') r.reset();   // not a BOM → put it back

    return r;
}

    /**
     * RFC 4180-compliant CSV split that handles quoted fields containing commas.
     * e.g. `"Inzlingen, Zoll","47.58557417"` → ["Inzlingen, Zoll", "47.58557417"]
     * Quotes are stripped from individual fields by {@link #strip}.
     */
    private static String[] csvSplitLine(String line) {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }

    /**
     * Parse HH:MM:SS into total seconds.
     * Hours may exceed 24 (Swiss GTFS uses operating-day semantics).
     */
    private static int parseHms(String s) {
        if (s == null || s.isBlank()) return 0;
        String[] p = s.trim().split(":");
        if (p.length < 3) return 0;
        return Integer.parseInt(p[0]) * 3600
             + Integer.parseInt(p[1]) * 60
             + Integer.parseInt(p[2]);
    }

    /** Strip surrounding whitespace and double-quotes. */
    private static String strip(String s) {
        if (s == null) return "";
        return s.trim().replace("\"", "");
    }

    private static double parseDouble(String s) {
        if (s == null || s.isBlank()) return 0.0;
        return Double.parseDouble(s);
    }

    private static int col(String[] h, String name) {
        for (int i = 0; i < h.length; i++)
            if (strip(h[i]).equalsIgnoreCase(name)) return i;
        throw new IllegalArgumentException("Required column not found: " + name);
    }

    private static int colOpt(String[] h, String name) {
        for (int i = 0; i < h.length; i++)
            if (strip(h[i]).equalsIgnoreCase(name)) return i;
        return -1;
    }
}