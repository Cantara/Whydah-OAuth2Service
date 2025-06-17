package net.whydah.service.health;

import net.whydah.demoservice.testsupport.TestServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;

import static io.restassured.RestAssured.given;

/**
 * @author <a href="mailto:asbjornwillersrud@gmail.com">Asbjørn Willersrud</a> 30/03/2016.
 */
public class HealthResourceTest {

    private TestServer testServer;

    @Before
    public void startServer() throws Exception {
        testServer = new TestServer(getClass());
        testServer.start();
        //Thread.sleep(15000);

    }

    @After
    public void stop() {
        testServer.stop();
    }

    @Test //TODO verify new health test
    @Ignore
    public void testHealth() throws IOException {
        given()
                .log().everything()
                .expect()
                .statusCode(HttpURLConnection.HTTP_OK)
                .log().everything()
                .when()
                .get(HealthResource.HEALTH_PATH);
    }

}