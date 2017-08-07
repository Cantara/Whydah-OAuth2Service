package net.whydah.demoservice.oauth2;

import net.whydah.util.Configuration;
import net.whydah.commands.oauth2.CommandVerifyToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.ws.rs.QueryParam;
import java.net.MalformedURLException;

public class OAuth2Resource {

    private static final Logger log = LoggerFactory.getLogger(OAuth2Resource.class);


    @RequestMapping("/oauth2")
    public String oauth2ResourceController(@QueryParam("code") String code, @QueryParam("state") String state) throws MalformedURLException {
        log.trace("oauth2 got code: {}",code);
        log.trace("oauth2 - got state: {}", state);

        String token = new CommandVerifyToken(Configuration.getString("oauth.uri"), code).execute();
        log.trace("oauth2 got verified token: {}", token);
        return "action";
    }

}
