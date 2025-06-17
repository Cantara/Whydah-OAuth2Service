package net.whydah.service.oauth2proxyserver;

import net.whydah.service.Main;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.*;

/**
 * Integration test that starts the actual application and tests real HTTP endpoints
 * This will definitively show if dependency injection is working
 */
public class ApplicationDependencyInjectionIntegrationTest {

    private Main application;
    private Thread applicationThread;
    private int testPort = 18080; // Use a different port for testing

    @Before
    public void startApplication() throws Exception {
        System.out.println("🚀 Starting actual application for dependency injection test...");

        // DON'T override the port - use the configured port from properties
        // System.setProperty("service.port", String.valueOf(testPort)); // Remove this

        // Set test properties
        System.setProperty("login.user", "test");
        System.setProperty("login.password", "test");

        // Start application
        application = new Main(); // Remove .withPort(testPort)
        applicationThread = new Thread(() -> {
            try {
                application.start();
            } catch (Exception e) {
                System.err.println("Failed to start application: " + e.getMessage());
                e.printStackTrace();
            }
        });

        applicationThread.start();

        // Wait for application to start and get the actual port
        int maxWait = 30;
        int waited = 0;
        while (!application.isStarted() && waited < maxWait) {
            Thread.sleep(1000);
            waited++;
            System.out.println("Waiting for application to start... " + waited + "s");
        }

        if (!application.isStarted()) {
            fail("Application failed to start within " + maxWait + " seconds");
        }

        // Get the actual port the application started on
        testPort = application.getPort();
        System.out.println("✅ Application started successfully on port " + testPort);
    }

    @After
    public void stopApplication() throws Exception {
        if (application != null) {
            application.stop();
            System.out.println("🛑 Application stopped");
        }

        if (applicationThread != null) {
            applicationThread.interrupt();
        }
    }

    @Test
    public void testOAuth2EndpointsWorkWithDependencyInjection() throws Exception {
        System.out.println("🔍 Testing OAuth2 endpoints with actual HTTP requests...");

        // Test endpoints that should work without complex business logic
        testEndpoint("/oauth2/health", "Health endpoint", false);
        testEndpoint("/oauth2/.well-known/openid-configuration", "OpenID Discovery endpoint", false);
        testEndpoint("/oauth2/.well-known/jwks.json", "JWKS endpoint", false);

        // Test endpoints that might have business logic issues but should show DI works
        testEndpoint("/oauth2/authorize?response_type=code&client_id=test&redirect_uri=http://localhost",
                "Authorize endpoint", true);
        testEndpoint("/oauth2/token?grant_type=authorization_code&client_id=test",
                "Token endpoint", true);
        testEndpoint("/oauth2/userinfo",
                "UserInfo endpoint", true);
        testEndpoint("/oauth2/logout?client_id=test",
                "Logout endpoint", true);

        System.out.println("🎉 All OAuth2 endpoints responded without dependency injection errors!");
    }

    private void testEndpoint(String path, String endpointName, boolean allowBusinessLogicErrors) throws Exception {
        String fullUrl = "http://localhost:" + testPort + path;
        System.out.println("Testing: " + endpointName + " - " + fullUrl);

        HttpURLConnection connection = null;
        try {
            URL url = new URL(fullUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            String response = readResponse(connection);

            // Always check for dependency injection specific errors
            assertFalse(endpointName + " response contains HK2 MultiException",
                    response.toLowerCase().contains("multiexception"));
            assertFalse(endpointName + " response contains UnsatisfiedDependencyException",
                    response.toLowerCase().contains("unsatisfieddependencyexception"));
            assertFalse(endpointName + " response contains 'no object available for injection'",
                    response.toLowerCase().contains("there was no object available"));
            assertFalse(endpointName + " response contains 'unable to perform operation: resolve'",
                    response.toLowerCase().contains("unable to perform operation: resolve"));

            if (allowBusinessLogicErrors) {
                // For complex endpoints, 500 might be business logic errors, not DI errors
                // Check that it's NOT a dependency injection error specifically
                if (responseCode == 500) {
                    // If it's a 500, make sure it's NOT a DI error
                    assertFalse("500 error appears to be dependency injection related: " + response,
                            isDependencyInjectionError(response));
                    System.out.println("⚠️  " + endpointName + " - Status: " + responseCode +
                            " (Business logic error, but DI works)");
                } else {
                    System.out.println("✅ " + endpointName + " - Status: " + responseCode +
                            " (No dependency injection errors)");
                }
            } else {
                // For simple endpoints, we expect them to work properly
                assertTrue(endpointName + " should return 200 OK", responseCode == 200);
                System.out.println("✅ " + endpointName + " - Status: " + responseCode +
                        " (Working perfectly)");
            }

        } catch (Exception e) {
            if (allowBusinessLogicErrors && e.getMessage().contains("500")) {
                System.out.println("⚠️  " + endpointName + " has business logic issues, but dependency injection works");
            } else {
                fail(endpointName + " failed with exception: " + e.getMessage());
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private boolean isDependencyInjectionError(String response) {
        String lowerResponse = response.toLowerCase();
        return lowerResponse.contains("multiexception") ||
                lowerResponse.contains("unsatisfieddependencyexception") ||
                lowerResponse.contains("there was no object available") ||
                lowerResponse.contains("unable to perform operation: resolve") ||
                lowerResponse.contains("injection");
    }

    @Test
    public void testActualDependencyInjectionOnFirstRequest() throws Exception {
        System.out.println("🔍 Testing ACTUAL dependency injection when resources are instantiated...");

        // Get the actual application port
        int appPort = 9898; // From the log output we saw

        // Test each endpoint to trigger actual bean instantiation
        testEndpointTriggersInjection(appPort, "/oauth2/health", "Health endpoint");
        testEndpointTriggersInjection(appPort, "/oauth2/.well-known/openid-configuration", "Discovery endpoint");
        testEndpointTriggersInjection(appPort, "/oauth2/authorize?response_type=code&client_id=test", "Authorize endpoint");
        testEndpointTriggersInjection(appPort, "/oauth2/token?grant_type=client_credentials&client_id=test", "Token endpoint");

        System.out.println("🎉 All endpoints responded - dependency injection works!");
    }

    private void testEndpointTriggersInjection(int port, String path, String endpointName) throws Exception {
        String fullUrl = "http://localhost:" + port + path;
        System.out.println("🔥 Triggering: " + endpointName + " - " + fullUrl);

        HttpURLConnection connection = null;
        try {
            URL url = new URL(fullUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            String response = readResponse(connection);

            // Key tests for dependency injection failures
            assertNotEquals(endpointName + " returned 500 - DEPENDENCY INJECTION FAILED",
                    500, responseCode);

            assertFalse(endpointName + " contains HK2 MultiException",
                    response.toLowerCase().contains("multiexception"));
            assertFalse(endpointName + " contains UnsatisfiedDependencyException",
                    response.toLowerCase().contains("unsatisfieddependencyexception"));
            assertFalse(endpointName + " contains injection failure",
                    response.toLowerCase().contains("there was no object available"));

            System.out.println("✅ " + endpointName + " - Status: " + responseCode +
                    " (No injection errors)");

        } catch (Exception e) {
            fail(endpointName + " failed: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void testEndpoint(String path, String endpointName) throws Exception {
        String fullUrl = "http://localhost:" + testPort + path;
        System.out.println("Testing: " + endpointName + " - " + fullUrl);

        HttpURLConnection connection = null;
        try {
            URL url = new URL(fullUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();

            // Key test: Should NOT be 500 Internal Server Error
            // 500 would indicate dependency injection failure
            assertNotEquals(endpointName + " returned 500 - likely dependency injection error",
                    500, responseCode);

            // Read response to check for dependency injection error messages
            String response = readResponse(connection);

            // Check for common dependency injection error indicators
            assertFalse(endpointName + " response contains dependency injection error",
                    response.toLowerCase().contains("unsatisfieddependencyexception"));
            assertFalse(endpointName + " response contains HK2 error",
                    response.toLowerCase().contains("multiexception"));
            assertFalse(endpointName + " response contains injection error",
                    response.toLowerCase().contains("injection"));

            System.out.println("✅ " + endpointName + " - Status: " + responseCode +
                    " (No dependency injection errors)");

        } catch (Exception e) {
            fail(endpointName + " failed with exception: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readResponse(HttpURLConnection connection) throws Exception {
        StringBuilder response = new StringBuilder();
        BufferedReader reader = null;

        try {
            // Try to read from normal input stream first
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        } catch (Exception e) {
            // If that fails, try error stream
            try {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            } catch (Exception e2) {
                return "Could not read response";
            }
        }

        if (reader != null) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
        }

        return response.toString();
    }

    @Test
    public void testHealthEndpointShowsProperWiring() throws Exception {
        System.out.println("🏥 Testing health endpoint for dependency injection status...");

        String healthUrl = "http://localhost:" + testPort + "/oauth2/health";
        HttpURLConnection connection = null;

        try {
            URL url = new URL(healthUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            assertEquals("Health endpoint should return 200", 200, responseCode);

            String response = readResponse(connection);
            System.out.println("Health response: " + response);

            // Health endpoint should show if wiring is working
            assertTrue("Health should show OK status", response.contains("\"Status\": \"OK\""));

            System.out.println("✅ Health endpoint confirms application is properly wired");

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}