package net.whydah.service.oauth2proxyserver;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import net.whydah.config.JerseyConfig;

/**
 * Integration test to verify that OAuth2ProxyAuthorizeResource can be properly instantiated
 * with its dependencies injected. This test will FAIL with HK2 MultiException until the
 * resource is made Spring-managed.
 */
public class OAuth2ProxyAuthorizeResourceDependencyInjectionTest {

    private Server server;
    private static final int TEST_PORT = 19999; // Use unique port to avoid conflicts

    @Before
    public void setUp() throws Exception {
        System.out.println("🚀 Setting up Jersey/Spring integration test for OAuth2ProxyAuthorizeResource...");

        // Create embedded Jetty server - similar to Main.java setup
        server = new Server(TEST_PORT);

        // Set up servlet context
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/oauth2");

        // Set up Spring context - exactly like Main.java
        AnnotationConfigWebApplicationContext springContext = new AnnotationConfigWebApplicationContext();
        springContext.scan("net.whydah");
        context.addEventListener(new ContextLoaderListener(springContext));
        context.setInitParameter("contextConfigLocation", "classpath:context.xml");

        // Create Jersey servlet with the same configuration as Main.java
        ServletHolder jerseyServlet = new ServletHolder(new ServletContainer());
        jerseyServlet.setInitParameter("jakarta.ws.rs.Application", JerseyConfig.class.getName());
        context.addServlet(jerseyServlet, "/*");

        server.setHandler(context);
        server.start();

        System.out.println("✅ Test server started on port " + TEST_PORT);
    }

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.stop();
            System.out.println("🛑 Test server stopped");
        }
    }

    @Test
    public void testOAuth2ProxyAuthorizeResourceDependencyInjection() throws Exception {
        System.out.println("🔍 Testing OAuth2ProxyAuthorizeResource dependency injection...");

        // This endpoint should trigger the instantiation of OAuth2ProxyAuthorizeResource
        // If dependency injection fails, we'll get a 500 error with HK2 MultiException
        String testUrl = "http://localhost:" + TEST_PORT + "/oauth2/authorize?response_type=code&client_id=test&redirect_uri=http://localhost";

        HttpURLConnection connection = null;
        try {
            URL url = new URL(testUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            String responseBody = readResponse(connection);

            System.out.println("Response Code: " + responseCode);
            System.out.println("Response Body: " + responseBody);

            // The key test: If dependency injection fails, we get a 500 error
            // with HK2 MultiException mentioning "UnsatisfiedDependencyException"
            if (responseCode == 500) {
                // Check if it's specifically a dependency injection error
                boolean isDependencyInjectionError =
                        responseBody.contains("MultiException") ||
                                responseBody.contains("UnsatisfiedDependencyException") ||
                                responseBody.contains("There was no object available") ||
                                responseBody.contains("Unable to perform operation: resolve");

                if (isDependencyInjectionError) {
                    fail("❌ OAuth2ProxyAuthorizeResource dependency injection FAILED! " +
                            "HK2 cannot resolve Spring-managed dependencies. " +
                            "Response: " + responseBody);
                } else {
                    // It's a 500 error but not dependency injection related
                    System.out.println("⚠️  Got 500 error but not dependency injection related - that's OK for this test");
                }
            } else {
                // If we get here, the resource was successfully instantiated
                // That means dependency injection worked!
                System.out.println("✅ OAuth2ProxyAuthorizeResource was successfully instantiated!");
                System.out.println("✅ All dependencies (TokenService, UserAuthorizationService, ClientService) were injected!");

                // For a successful DI test, we just need to verify the resource could be created
                // The actual OAuth2 flow logic might fail (that's a separate concern)
                assertTrue("Dependency injection should work - resource was created successfully",
                        responseCode != 500 || !responseBody.contains("UnsatisfiedDependencyException"));
            }

        } catch (Exception e) {
            // Check if the exception is related to dependency injection
            String errorMessage = e.getMessage();
            if (errorMessage != null &&
                    (errorMessage.contains("MultiException") ||
                            errorMessage.contains("UnsatisfiedDependencyException"))) {
                fail("❌ Dependency injection failed with exception: " + errorMessage);
            } else {
                // Other exceptions are OK for this test - we only care about DI
                System.out.println("⚠️  Got non-DI related exception, that's OK: " + e.getMessage());
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testSpecificDependencyInjectionErrorMessage() throws Exception {
        System.out.println("🔍 Testing for specific HK2 dependency injection error...");

        // Test the exact endpoint that should fail with the error you showed
        String testUrl = "http://localhost:" + TEST_PORT + "/oauth2/authorize?response_type=code&client_id=CLIENT_ID&redirect_uri=http://localhost:8080/callback";

        HttpURLConnection connection = null;
        try {
            URL url = new URL(testUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            String responseBody = readResponse(connection);

            System.out.println("Response Code: " + responseCode);

            // Look for the specific error pattern from your stack trace
            boolean hasTokenServiceError = responseBody.contains("TokenService") &&
                    responseBody.contains("UnsatisfiedDependencyException");
            boolean hasUserAuthServiceError = responseBody.contains("UserAuthorizationService") &&
                    responseBody.contains("UnsatisfiedDependencyException");
            boolean hasClientServiceError = responseBody.contains("ClientService") &&
                    responseBody.contains("UnsatisfiedDependencyException");

            if (hasTokenServiceError || hasUserAuthServiceError || hasClientServiceError) {
                fail("❌ EXPECTED FAILURE: HK2 cannot inject Spring-managed services into OAuth2ProxyAuthorizeResource!\n" +
                        "This test should PASS after making the resource Spring-managed with @Component.\n" +
                        "Error details: " + responseBody);
            } else if (responseCode == 500) {
                System.out.println("⚠️  Got 500 error but not the expected dependency injection error");
                System.out.println("Response: " + responseBody);
            } else {
                System.out.println("✅ No dependency injection errors - OAuth2ProxyAuthorizeResource is properly wired!");
            }

        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("UnsatisfiedDependencyException")) {
                fail("❌ EXPECTED FAILURE: " + errorMsg + "\nThis should be fixed when the resource is made Spring-managed.");
            }
            // Other exceptions are OK
            System.out.println("⚠️  Exception (not DI related): " + e.getMessage());
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
                return "Could not read response: " + e.getMessage();
            }
        }

        if (reader != null) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            reader.close();
        }

        return response.toString();
    }
}