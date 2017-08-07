package no.cantara.service.testsupport;

import com.jayway.restassured.RestAssured;
import no.cantara.service.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TestServer {

    private static final Logger log = LoggerFactory.getLogger(TestServer.class);


    private Main main;


    private String url;
    private Class testClass;

    public TestServer(Class testClass) {
        this.testClass = testClass;
    }


    public void start() throws InterruptedException {
        new Thread(() -> {
            main = new Main();
            main.start();
        }).start();
        do {
            Thread.sleep(10);
        } while (main == null || !main.isStarted());
        RestAssured.port = main.getPort();

        RestAssured.basePath = Main.CONTEXT_PATH;
        url = "http://localhost:" + main.getPort() + Main.CONTEXT_PATH ;
    }

    public void stop() {
        main.stop();
    }

    public String getUrl() {
        return "http://localhost:" + main.getPort() + Main.CONTEXT_PATH;
    }
}
