package net.whydah.service.health;

import io.restassured.response.Response;
import net.whydah.service.Main;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for verifying Spring/Jersey integration with the HealthResource
 */
public class HealthResourceSpringIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(HealthResourceSpringIntegrationTest.class);
    private static final int TEST_PORT = 9898;
    private static ExecutorService executorService;
    private static Process serverProcess;

    @BeforeClass
    public static void startServer() throws Exception {
        // Set system property for port
        System.setProperty("service.port", String.valueOf(TEST_PORT));

        // Start server in a separate process
        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-Dservice.port=" + TEST_PORT,
                "-cp",
                System.getProperty("java.class.path"),
                "net.whydah.service.Main");

        serverProcess = pb.start();

        // Start a thread to consume the process output
        executorService = Executors.newFixedThreadPool(2);
        executorService.submit(() -> {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(serverProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[SERVER] " + line);
                }
            } catch (IOException e) {
                log.error("Error reading server output", e);
            }
        });

        executorService.submit(() -> {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(serverProcess.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.err.println("[SERVER] " + line);
                }
            } catch (IOException e) {
                log.error("Error reading server error output", e);
            }
        });

        // Verify server is actually running by trying to connect to the port
        boolean serverStarted = false;
        for (int i = 0; i < 30; i++) { // 30 seconds max
            try {
                Thread.sleep(1000); // Check once per second
                try (Socket socket = new Socket("localhost", TEST_PORT)) {
                    if (socket.isConnected()) {
                        serverStarted = true;
                        break;
                    }
                } catch (IOException e) {
                    // Server not up yet, continue waiting
                    log.info("Waiting for server to start...");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (!serverStarted) {
            stopServer(); // Clean up resources
            throw new RuntimeException("Server failed to start within timeout period");
        }

        // Configure RestAssured
        io.restassured.RestAssured.port = TEST_PORT;
        io.restassured.RestAssured.basePath = Main.CONTEXT_PATH;

        // Give the server a little more time to fully initialize
        Thread.sleep(2000);

        log.info("Server started successfully for tests");
    }

    @AfterClass
    public static void stopServer() {
        // Stop the server process
        if (serverProcess != null && serverProcess.isAlive()) {
            serverProcess.destroy();
            try {
                if (!serverProcess.waitFor(5, TimeUnit.SECONDS)) {
                    serverProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for server process to terminate", e);
                serverProcess.destroyForcibly();
            }
        }

        // Shut down the executor service
        if (executorService != null) {
            executorService.shutdownNow();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("Executor service did not terminate in time");
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for executor service to terminate", e);
            }
        }

        log.info("Server stopped");
    }

    @Test
    public void testHealthEndpointWithSpringIntegration() throws IOException {
        log.info("Testing health endpoint");

        // Send request to health endpoint and validate response
        Response response = given()
                .log().everything()
                .expect()
                .statusCode(HttpURLConnection.HTTP_OK)
                .log().everything()
                .when()
                .get(HealthResource.HEALTH_PATH);

        // Parse response body
        String responseBody = response.getBody().asString();

        // Verify response contains expected JSON fields
        assertNotNull("Response body should not be null", responseBody);
        assertTrue("Response should contain Status field", responseBody.contains("\"Status\""));
        assertTrue("Response should contain Version field", responseBody.contains("\"Version\""));
        assertTrue("Response should contain now field", responseBody.contains("\"now\""));
        assertTrue("Response should contain IP field", responseBody.contains("\"IP\""));

        // More detailed validation using JSON path
        response.then()
                .body("Status", equalTo("OK"));

        log.info("Health endpoint successfully responded with valid data. Spring/Jersey integration is working.");
    }
}