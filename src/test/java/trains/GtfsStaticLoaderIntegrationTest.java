package ch.uzh.ifi.hase.soprafs26.trains;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for GtfsStaticLoader download functionality.
 * Tests actual HTTP download from a real URL (configurable via system properties or env vars).
 *
 * To run: Set the GTFS_STATIC_URL and optionally GTFS_API_TOKEN environment variables, then run:
 *   GTFS_STATIC_URL=https://... GTFS_API_TOKEN=... ./gradlew test --tests "*GtfsStaticLoaderIntegrationTest*"
 *
 * Or set system properties:
 *   ./gradlew test --tests "*GtfsStaticLoaderIntegrationTest*" -Dgtfs.static-url=https://...
 */
public class GtfsStaticLoaderIntegrationTest {

    private GtfsDataStore gtfsDataStore;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Create a real GtfsDataStore instance
        gtfsDataStore = new GtfsDataStore();
    }

    /**
     * Test that GtfsStaticLoader successfully downloads the ZIP file from the configured real URL.
     * 
     * Configure the test by setting:
     * - Environment variable: GTFS_STATIC_URL=https://your-gtfs-url/data.zip
     * - Environment variable: GTFS_API_TOKEN=your-token (optional)
     * 
     * Or system properties:
     * - -Dgtfs.static-url=https://...
     * - -Dgtfs.api-token=...
     */
    //
    void testDownloadZip_SuccessfullyDownloadsFromRealUrl() throws Exception {
        // Get configuration from environment or system properties
        String staticUrl = System.getProperty("gtfs.static-url", 
            System.getenv("GTFS_STATIC_URL"));
        String apiToken = System.getProperty("gtfs.api-token", 
            System.getenv("GTFS_API_TOKEN"));

        // Skip test if URL not configured
        if (staticUrl == null || staticUrl.isEmpty()) {
            System.out.println("Skipping test: Set GTFS_STATIC_URL env var or -Dgtfs.static-url system property");
            return;
        }

        System.out.println("Downloading from: " + staticUrl);

        // Create destination path
        Path destZip = tempDir.resolve("downloaded-gtfs.zip");
        System.out.println("Downloading to: " + destZip.toAbsolutePath());

        // Create the loader
        GtfsStaticLoader loader = new GtfsStaticLoader(gtfsDataStore);

        // Use reflection to set the static URL field
        var staticUrlField = GtfsStaticLoader.class.getDeclaredField("staticUrl");
        staticUrlField.setAccessible(true);
        staticUrlField.set(loader, staticUrl);

        // Set API token if provided
        if (apiToken != null && !apiToken.isEmpty()) {
            var apiTokenField = GtfsStaticLoader.class.getDeclaredField("apiToken");
            apiTokenField.setAccessible(true);
            apiTokenField.set(loader, apiToken);
        }

        // Download using reflection
        var downloadMethod = GtfsStaticLoader.class.getDeclaredMethod("downloadZip", Path.class);
        downloadMethod.setAccessible(true);

        // This performs the actual HTTP download from the real URL
        assertDoesNotThrow(() -> downloadMethod.invoke(loader, destZip),
            "Download should succeed");

        // Verify the file was downloaded
        assertTrue(Files.exists(destZip), "ZIP file should be downloaded to disk");

        // Verify it's a valid ZIP file
        long fileSize = Files.size(destZip);
        assertTrue(fileSize > 0, "Downloaded file should not be empty (size: " + fileSize + " bytes)");

        // Basic ZIP validation: starts with PK (0x504B)
        byte[] header = Files.readAllBytes(destZip);
        if (header.length >= 2) {
            assertEquals((byte) 0x50, header[0], "File should start with 'P' (ZIP magic number)");
            assertEquals((byte) 0x4B, header[1], "File should start with 'K' (ZIP magic number)");
        }

        System.out.println("Successfully downloaded " + fileSize + " bytes");
        System.out.println("File location: " + destZip.toAbsolutePath());
        System.out.println("NOTE: This file will be deleted when the test finishes (JUnit temp directory)");
        System.out.println("To keep it, copy it to a permanent location before the test ends");
    }


    
    void testDownloadZip_andParse() throws Exception {
        // This test will actually download and parse the GTFS static data
        // It requires a valid URL and token, and may take a few minutes to run

        String staticUrl = System.getProperty("gtfs.static-url", 
            System.getenv("GTFS_STATIC_URL"));
        String apiToken = System.getProperty("gtfs.api-token", 
            System.getenv("GTFS_API_TOKEN"));

        if (staticUrl == null || staticUrl.isEmpty()) {
            System.out.println("Skipping test: Set GTFS_STATIC_URL env var or -Dgtfs.static-url system property");
            return;
        }

        GtfsStaticLoader loader = new GtfsStaticLoader(gtfsDataStore);

        // Use reflection to set all required fields
        var staticUrlField = GtfsStaticLoader.class.getDeclaredField("staticUrl");
        staticUrlField.setAccessible(true);
        staticUrlField.set(loader, staticUrl);

        if (apiToken != null && !apiToken.isEmpty()) {
            var apiTokenField = GtfsStaticLoader.class.getDeclaredField("apiToken");
            apiTokenField.setAccessible(true);
            apiTokenField.set(loader, apiToken);
        }

        // Set the local zip path to a PERMANENT location so you can view it after the test
        Path permanentDir = Path.of(System.getProperty("user.home"), "gtfs-test-downloads");
        Files.createDirectories(permanentDir);
        Path localZipPath = permanentDir.resolve("gtfs-static-" + System.currentTimeMillis() + ".zip");
        
        var localZipPathField = GtfsStaticLoader.class.getDeclaredField("localZipPath");
        localZipPathField.setAccessible(true);
        localZipPathField.set(loader, localZipPath.toString());

        System.out.println("Downloading from: " + staticUrl);
        System.out.println("Saving to: " + localZipPath.toAbsolutePath());
        System.out.println("NOTE: This file will be saved PERMANENTLY in your home directory");

        // This will download and parse the GTFS data
        assertDoesNotThrow(() -> loader.downloadAndParse(),
            "Download and parse should complete without exceptions");

        // Verify the file was downloaded
        assertTrue(Files.exists(localZipPath), "ZIP file should exist after download");

        // Verify that data was loaded into the store
        assertNotNull(gtfsDataStore.loadedAt(), "Data store should have a loaded timestamp after parsing");
        assertNotNull(gtfsDataStore.tripNames(), "Trips map should not be null");
        assertNotNull(gtfsDataStore.stops(), "Stops map should not be null");
        assertTrue(gtfsDataStore.tripNames().size() > 0, "Should have loaded trip names");
        assertTrue(gtfsDataStore.stops().size() > 0, "Should have loaded stops");

        System.out.println("Successfully downloaded and parsed GTFS data");
        System.out.println("Loaded " + gtfsDataStore.stops().size() + " stops");
        System.out.println("Loaded " + gtfsDataStore.tripNames().size() + " trips");
        System.out.println("Loaded " + gtfsDataStore.tripShapes().size() + " trip shapes");

        //System.out.println("PERMANENT FILE LOCATION: " + localZipPath.toAbsolutePath());

    }

    @Test
    void testParsePermanentZip() throws Exception {
        // This test assumes you have a valid GTFS ZIP file at the specified location
        // It will test the parsing logic without downloading

        String localZipPathStr = System.getProperty("gtfs.local-zip-path", 
            System.getenv("GTFS_LOCAL_ZIP_PATH"));
        
        if (localZipPathStr == null || localZipPathStr.isEmpty()) {
            System.out.println("Skipping test: Set GTFS_LOCAL_ZIP_PATH env var or -Dgtfs.local-zip-path system property");
            return;
        }

        Path localZipPath = Path.of(localZipPathStr);
        if (!Files.exists(localZipPath)) {
            System.out.println("Skipping test: Local ZIP file does not exist at " + localZipPath);
            return;
        }

        GtfsStaticLoader loader = new GtfsStaticLoader(gtfsDataStore);

        // Use reflection to set the local zip path
        var localZipPathField = GtfsStaticLoader.class.getDeclaredField("localZipPath");
        localZipPathField.setAccessible(true);
        localZipPathField.set(loader, localZipPath.toString());
        System.out.println("Parsing local ZIP file: " + localZipPath.toAbsolutePath());     

        // This will parse the GTFS data from the local ZIP file
        assertDoesNotThrow(() -> loader.parseAndLoad(localZipPath),  
            "Parsing local ZIP should complete without exceptions");

        // Verify that data was loaded into the store
        assertNotNull(gtfsDataStore.loadedAt(), "Data store should have a loaded timestamp after parsing");
        assertNotNull(gtfsDataStore.tripNames(), "Trips map should not be null");
        assertNotNull(gtfsDataStore.stops(), "Stops map should not be null");
        assertNotNull(gtfsDataStore.stopTimes(), "StopTimes map should not be null");

        assertTrue(gtfsDataStore.tripNames().size() > 0, "Should have loaded trip names");
        assertTrue(gtfsDataStore.stops().size() > 0, "Should have loaded stops");
        assertTrue(gtfsDataStore.stopTimes().size() > 0, "Should have loaded stop times");
        

        System.out.println("Successfully downloaded and parsed GTFS data");
        System.out.println("Loaded " + gtfsDataStore.stops().size() + " stops");
        System.out.println("Loaded " + gtfsDataStore.tripNames().size() + " trips");
        System.out.println("Loaded " + gtfsDataStore.stopTimes().size() + " stop times");

        //System.out.println("PERMANENT FILE LOCATION: " + localZipPath.toAbsolutePath()); 
        }
    /**
     * Test that the download creates parent directories if they don't exist.
     */
    //
    void testDownloadZip_CreatesNestedDirectories() throws Exception {
        String staticUrl = System.getProperty("gtfs.static-url", 
            System.getenv("GTFS_STATIC_URL"));
        String apiToken = System.getProperty("gtfs.api-token", 
            System.getenv("GTFS_API_TOKEN"));

        if (staticUrl == null || staticUrl.isEmpty()) {
            System.out.println("Skipping test: Set GTFS_STATIC_URL env var or -Dgtfs.static-url system property");
            return;
        }

        // Create a nested path
        Path nestedPath = tempDir.resolve("data/gtfs/2024/static.zip");
        System.out.println("Downloading to nested path: " + nestedPath.toAbsolutePath());

        GtfsStaticLoader loader = new GtfsStaticLoader(gtfsDataStore);

        var staticUrlField = GtfsStaticLoader.class.getDeclaredField("staticUrl");
        staticUrlField.setAccessible(true);
        staticUrlField.set(loader, staticUrl);

        if (apiToken != null && !apiToken.isEmpty()) {
            var apiTokenField = GtfsStaticLoader.class.getDeclaredField("apiToken");
            apiTokenField.setAccessible(true);
            apiTokenField.set(loader, apiToken);
        }

        var downloadMethod = GtfsStaticLoader.class.getDeclaredMethod("downloadZip", Path.class);
        downloadMethod.setAccessible(true);

        assertDoesNotThrow(() -> downloadMethod.invoke(loader, nestedPath));

        assertTrue(Files.exists(nestedPath), "Nested directories should be created and file downloaded");
        assertTrue(Files.isRegularFile(nestedPath), "File should be a regular file");
        assertTrue(Files.size(nestedPath) > 0, "Downloaded file should not be empty");
        
        System.out.println("Nested download successful. File location: " + nestedPath.toAbsolutePath());
        System.out.println("NOTE: This file will be deleted when the test finishes (JUnit temp directory)");
    }
}

