package net.whydah.service.oauth2proxyserver;

import net.whydah.demoservice.testsupport.TestServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;

import static io.restassured.RestAssured.given;

public class OAuth2ProxyServerResourceTest {

    private static TestServer testServer;

    @BeforeClass
    public static void startServer() throws Exception {
        testServer = new TestServer(OAuth2ProxyServerResourceTest.class);
        testServer.start();
    }

    @AfterClass
    public static void stop() {
        if (testServer != null) {
            testServer.stop();
        }
    }

    //TODO re-enable test which inject "code"
    @Test
    @Ignore("Test disabled - needs code injection implementation")
    public void testOAuth2StubbedServerRunning() throws IOException {
        given()
                .log().everything()
                .expect()
                .statusCode(HttpURLConnection.HTTP_OK)
                .log().everything()
                .when()
                .get(OAuth2ProxyTokenResource.OAUTH2TOKENSERVER_PATH);
    }

    @Test
    public void testOAuth2StubbedServerProtection() throws IOException {
        given()
                .log().everything()
                .expect()
                .statusCode(HttpURLConnection.HTTP_BAD_REQUEST)
                .log().everything()
                .when()
                .post(OAuth2ProxyTokenResource.OAUTH2TOKENSERVER_PATH);
    }
}