package net.whydah.commands.oauth2;

import net.whydah.service.testsupport.TestServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.junit.Assert.assertTrue;

public class CommandGetOAuth2AccessTokenTest {

    private final static Logger log = LoggerFactory.getLogger(CommandGetOAuth2AccessTokenTest.class);


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
    public void testCommandOAuth2TokenVerifier() throws Exception {

        log.trace("Calling {}",testServer.getUrl());
        String code = "myDummyCode";
        String redirectURI="https://www.vg.no";
        String access_token = new CommandGetOAuth2AccessToken(testServer.getUrl(), code, redirectURI).execute();
        log.debug("Returned access_token: " + access_token);
        assertTrue(access_token != null);
        assertTrue(access_token.length() > 10);
        assertTrue(access_token.contains("token_type"));
    }


}
