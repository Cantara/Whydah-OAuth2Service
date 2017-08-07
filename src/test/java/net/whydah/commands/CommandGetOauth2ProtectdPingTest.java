package net.whydah.commands;

import net.whydah.service.testsupport.TestServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.junit.Assert.assertTrue;

public class CommandGetOauth2ProtectdPingTest {
    private final static Logger log = LoggerFactory.getLogger(CommandGetOauth2ProtectdPingTest.class);


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
    public void testCommandGetOauth2ProtectdPing() throws Exception {


        log.trace("Calling {}", testServer.getUrl());
        String returned_data = new CommandGetOauth2ProtectedPing(testServer.getUrl()).execute();
        log.debug("Returned: " + returned_data);
        assertTrue(returned_data != null);
        assertTrue(returned_data.contains("PONG"));
    }
}