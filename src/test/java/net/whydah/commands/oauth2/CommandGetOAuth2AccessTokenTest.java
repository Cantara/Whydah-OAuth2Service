package net.whydah.commands.oauth2;

import net.whydah.demoservice.testsupport.TestServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

public class CommandGetOAuth2AccessTokenTest {

    private final static Logger log = LoggerFactory.getLogger(CommandGetOAuth2AccessTokenTest.class);

    private static TestServer testServer;

    @BeforeClass
    public static void startServer() throws Exception {
        testServer = new TestServer(CommandGetOAuth2AccessTokenTest.class);
        testServer.start();
    }

    @AfterClass
    public static void stop() {
        if (testServer != null) {
            testServer.stop();
        }
    }

    @Test
    @org.junit.Ignore("TODO need to work on this test")
    public void testCommandOAuth2TokenVerifier() throws Exception {
        log.trace("Calling {}", testServer.getUrl());
        String code = "myDummyCode";
        String redirectURI = "https://www.vg.no";
        String access_token = new CommandGetOAuth2AccessToken(testServer.getUrl(), code, redirectURI).execute();
        log.debug("Returned access_token: " + access_token);
        assertTrue(access_token != null);
        assertTrue(access_token.length() > 10);
        assertTrue(access_token.contains("token_type"));
    }
}