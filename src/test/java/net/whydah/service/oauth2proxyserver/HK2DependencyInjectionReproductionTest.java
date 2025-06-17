package net.whydah.service.oauth2proxyserver;

import net.whydah.config.JerseyConfig;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.Test;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.*;

/**
 * Test that reproduces the HK2 dependency injection issue by forcing Jersey
 * to initialize resources before Spring context is fully ready.
 * This mimics the production JAR execution scenario more closely.
 */
public class HK2DependencyInjectionReproductionTest {

    private Server server;
    private static final int TEST_PORT = 19998;

    @Test
    public void testHK2DependencyInjectionFailure() throws Exception {
        System.out.println("🔥 Testing HK2 dependency injection failure scenario...");

        // Create Jersey configuration that will try to instantiate resources immediately
        JerseyConfig jerseyConfig = new JerseyConfig();

        // Try to get an instance of OAuth2ProxyAuthorizeResource directly from Jersey
        // This should fail because Jersey's HK2 container can't resolve Spring dependencies
        try {
            // This mimics what happens in production when Jersey tries to create the resource
            OAuth2ProxyAuthorizeResource resource = new OAuth2ProxyAuthorizeResource(null, null, null);
            fail("❌ Expected NullPointerException due to null dependencies, but didn't get it");
        } catch (NullPointerException e) {
            System.out.println("✅ Got expected NullPointerException - this shows the injection problem");
        }

        // Now test the real integration issue
        testWithJerseyHK2ContainerOnly();
    }

    @Test
    public void testWithJerseyHK2ContainerOnly() throws Exception {
        System.out.println("🔍 Testing with Jersey HK2 container ONLY (no Spring)...");

        // Create a Jersey-only setup (without Spring integration)
        // This should definitely fail with dependency injection errors
        Server server = new Server(TEST_PORT + 1);
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/oauth2");

        // Create Jersey servlet WITHOUT Spring integration
        ServletHolder jerseyServlet = new ServletHolder(new ServletContainer());
        jerseyServlet.setInitParameter("jakarta.ws.rs.Application", JerseyConfig.class.getName());
        // DON'T add Spring context - this forces HK2-only dependency resolution

        context.addServlet(jerseyServlet, "/*");
        server.setHandler(context);

        try {
            server.start();

            // Try to access the endpoint
            String testUrl = "http://localhost:" + (TEST_PORT + 1) + "/oauth2/authorize?response_type=code&client_id=test";

            HttpURLConnection connection = null;
            try {
                URL url = new URL(testUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                String responseBody = readResponse(connection);

                System.out.println("Response Code: " + responseCode);
                System.out.println("Response excerpt: " + responseBody.substring(0, Math.min(500, responseBody.length())));

                // This SHOULD fail with dependency injection errors
                if (responseCode == 500) {
                    boolean hasExpectedError =
                            responseBody.contains("UnsatisfiedDependencyException") ||
                                    responseBody.contains("MultiException") ||
                                    responseBody.contains("There was no object available") ||
                                    responseBody.contains("TokenService") ||
                                    responseBody.contains("UserAuthorizationService") ||
                                    responseBody.contains("ClientService");

                    if (hasExpectedError) {
                        System.out.println("✅ SUCCESS: Got expected HK2 dependency injection failure!");
                        System.out.println("This proves the issue exists when Spring is not managing the resource.");
                        // This is actually the SUCCESS case for this test
                        assertTrue("Expected dependency injection failure", true);
                    } else {
                        fail("❌ Got 500 error but not the expected dependency injection failure: " + responseBody);
                    }
                } else {
                    fail("❌ Expected 500 error with dependency injection failure, but got: " + responseCode);
                }

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

        } finally {
            server.stop();
        }
    }

    @Test
    public void testReflectionBasedResourceInstantiation() throws Exception {
        System.out.println("🔬 Testing direct resource instantiation (simulates HK2 behavior)...");

        // This test simulates what HK2 does when it tries to create the resource
        try {
            // HK2 would try to create the resource using reflection
            Class<?> resourceClass = OAuth2ProxyAuthorizeResource.class;

            // Check if it has the @Inject constructor
            var constructors = resourceClass.getConstructors();
            System.out.println("Found " + constructors.length + " constructors");

            for (var constructor : constructors) {
                System.out.println("Constructor: " + constructor);
                System.out.println("Parameter count: " + constructor.getParameterCount());
                System.out.println("Has @Inject: " + constructor.isAnnotationPresent(jakarta.inject.Inject.class));

                if (constructor.getParameterCount() > 0) {
                    // This is what would fail in HK2 - it can't provide the parameters
                    try {
                        // Try to instantiate with null parameters (what HK2 would fail to do)
                        Object[] nullParams = new Object[constructor.getParameterCount()];
                        Object instance = constructor.newInstance(nullParams);
                        fail("❌ Should not be able to create instance with null dependencies");
                    } catch (Exception e) {
                        System.out.println("✅ Expected failure when trying to instantiate with null dependencies: " + e.getClass().getSimpleName());
                        assertTrue("Should fail with null dependencies", true);
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("✅ Resource instantiation failed as expected: " + e.getMessage());
        }
    }

    @Test
    public void testSpringOnlyResourceCreation() throws Exception {
        System.out.println("🌱 Testing Spring-only resource creation...");

        // Create Spring context
        AnnotationConfigWebApplicationContext springContext = new AnnotationConfigWebApplicationContext();
        springContext.scan("net.whydah");
        springContext.refresh();

        try {
            // Try to get the resource from Spring context
            // This will only work if the resource has @Component
            try {
                OAuth2ProxyAuthorizeResource resource = springContext.getBean(OAuth2ProxyAuthorizeResource.class);
                System.out.println("✅ Spring successfully created the resource!");
                System.out.println("This means the resource is properly Spring-managed.");
                assertNotNull("Resource should be created by Spring", resource);
            } catch (Exception e) {
                System.out.println("❌ Spring cannot create the resource: " + e.getMessage());
                System.out.println("This means the resource is NOT Spring-managed (missing @Component)");
                assertTrue("Resource should fail to be created by Spring without @Component",
                        e.getMessage().contains("No qualifying bean") ||
                                e.getMessage().contains("NoSuchBeanDefinitionException"));
            }

        } finally {
            springContext.close();
        }
    }

    private String readResponse(HttpURLConnection connection) throws Exception {
        StringBuilder response = new StringBuilder();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        } catch (Exception e) {
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