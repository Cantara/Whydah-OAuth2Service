package net.whydah.commands.oauth2;

import net.whydah.demoservice.testsupport.TestServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

public class CommandAuthorizeOAuth2ApplicationTest {

    private final static Logger log = LoggerFactory.getLogger(CommandAuthorizeOAuth2ApplicationTest.class);

    private static TestServer testServer;

    @BeforeClass
    public static void startServer() throws Exception {
        testServer = new TestServer(CommandAuthorizeOAuth2ApplicationTest.class);
        testServer.start();
    }

    @AfterClass
    public static void stop() {
        if (testServer != null) {
            testServer.stop();
        }
    }

    @Test
    @org.junit.Ignore("Disabled as it needs real data to work")
    public void testCommandAuthorizeOAuth2Application() throws Exception {
        log.trace("Calling {}", testServer.getUrl());
        String access_token = new CommandAuthorizeOAuth2Application(testServer.getUrl()).execute();
        log.debug("Returned access_token: " + access_token);
        assertTrue(access_token != null);
        assertTrue(access_token.length() > 10);
        assertTrue(access_token.contains("access_token"));
    }
}