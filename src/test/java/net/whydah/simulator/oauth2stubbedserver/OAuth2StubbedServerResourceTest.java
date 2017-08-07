package net.whydah.simulator.oauth2stubbedserver;

import net.whydah.service.testsupport.TestServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.HttpURLConnection;

import static com.jayway.restassured.RestAssured.given;

public class OAuth2StubbedServerResourceTest {

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
    public void testOAuth2StubbedServerRunning() throws IOException {
        given()
                .log().everything()
                .expect()
                .statusCode(HttpURLConnection.HTTP_OK)
                .log().everything()
                .when()
                .get(OAuth2StubbedServerResource.OAUTH2TOKENSERVER_PATH);
    }

    @Test
    public void testOAuth2StubbedServerProtection() throws IOException {
        given()
                .log().everything()
                .expect()
                .statusCode(HttpURLConnection.HTTP_FORBIDDEN)
                .log().everything()
                .when()
                .post(OAuth2StubbedServerResource.OAUTH2TOKENSERVER_PATH);
    }

}