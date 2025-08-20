package net.whydah.service.health;

import static org.junit.Assert.assertEquals;

import java.net.HttpURLConnection;
import java.net.URL;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.whydah.config.JerseyConfig;

/**
 * Test for verifying Spring/Jersey integration with a simple health endpoint
 */
public class JerseySpringIntegrationTest {

    private Server server;
    private static final int TEST_PORT = 9898;

    @Path("/test-health")
    public static class TestHealthResource {
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response healthCheck() {
            return Response.ok("{\"status\":\"OK\"}").build();
        }
    }

    @Configuration
    static class TestConfig {
        @Bean
        public TestHealthResource testHealthResource() {
            return new TestHealthResource();
        }

        @Bean
        public JerseyConfig jerseyConfig() {
            return new JerseyConfig();
        }
    }

    @Before
    public void setUp() throws Exception {
        // Create server
        server = new Server(TEST_PORT);

        // Set up servlet context
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/test");

        // Set up Spring context
        AnnotationConfigWebApplicationContext springContext = new AnnotationConfigWebApplicationContext();
        springContext.register(TestConfig.class);
        springContext.register(JerseyConfig.class); // Also register JerseyConfig directly
        context.addEventListener(new ContextLoaderListener(springContext));

        // Create Jersey servlet
        ServletHolder jerseyServlet = new ServletHolder(new ServletContainer());
        jerseyServlet.setInitParameter("jakarta.ws.rs.Application", JerseyConfig.class.getName());
        context.addServlet(jerseyServlet, "/*");

        server.setHandler(context);
        server.start();

        System.out.println("Test server started on port " + TEST_PORT);
    }

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.stop();
            System.out.println("Test server stopped");
        }
    }

    @Test
    public void testSpringJerseyIntegration() throws Exception {
        // Connect to the test health endpoint
        URL url = new URL("http://localhost:" + TEST_PORT + "/test/test-health");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Verify response
        int responseCode = connection.getResponseCode();
        assertEquals(HttpURLConnection.HTTP_OK, responseCode);

        // Read response body if needed
        java.io.BufferedReader in = new java.io.BufferedReader(
                new java.io.InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        System.out.println("Response: " + content.toString());
    }
}