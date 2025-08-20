package net.whydah.service.oauth2proxyserver;

import java.nio.charset.Charset;
import java.util.Base64;

import org.junit.Test;

public class Base64EncodingAndDecodingTest {

    String CLIENT_ID="my test id";
    String CLIENT_SECRET="my test secret";
    @Test
    public void testbase64EncodingAndDecoding() throws Exception{
        String authString = CLIENT_ID + ":" + CLIENT_SECRET;
        String encoding = Base64.getEncoder().encodeToString(authString.getBytes());

        String basicAuth="Basic "+encoding;
        String base64Credentials = basicAuth.substring("Basic".length()).trim();
        String credentials = new String(Base64.getDecoder().decode(base64Credentials),
                Charset.forName("UTF-8"));

    }
}
