package net.whydah.service.oauth2ping;

import net.whydah.service.testsupport.TestServer;
import net.whydah.commands.config.ConstantValue;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.HttpURLConnection;

import static com.jayway.restassured.RestAssured.given;

public class PingResourceTest {


    private TestServer testServer;

    @BeforeClass
    public void startServer() throws Exception {
        testServer = new TestServer(getClass());
        testServer.start();
    }

    @AfterClass
    public void stop() {
        testServer.stop();
    }

    @Test
    public void testOauth2Ping() throws IOException {
        given()
                .header("Authorization", "Bearer " + ConstantValue.ATOKEN)
                .log().everything()
                .expect()
                .statusCode(HttpURLConnection.HTTP_OK)
                .log().everything()
                .when()
                .get(PingResource.PING_PATH);
    }

}
