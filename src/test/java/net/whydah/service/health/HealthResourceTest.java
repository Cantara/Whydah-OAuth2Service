package net.whydah.service.health;

import net.whydah.demoservice.testsupport.TestServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.HttpURLConnection;

import static com.jayway.restassured.RestAssured.given;

/**
 * @author <a href="mailto:asbjornwillersrud@gmail.com">Asbjørn Willersrud</a> 30/03/2016.
 */
public class HealthResourceTest {

    private TestServer testServer;

    @BeforeClass
    public void startServer() throws Exception {
        testServer = new TestServer(getClass());
        testServer.start();
        //Thread.sleep(15000);

    }

    @AfterClass
    public void stop() {
        testServer.stop();
    }

    @Test(enabled = false) //TODO verify new health test
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