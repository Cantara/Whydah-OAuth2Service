package net.whydah.demoservice.oauth2;

import net.whydah.util.Configuration;
import net.whydah.util.CookieManager;
import net.whydah.commands.config.ConstantValue;
import net.whydah.commands.oauth2.CommandVerifyToken;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
    
    @RequestMapping("/logout")
    public String logout( @QueryParam("redirect_uri") String redirect_uri, HttpServletRequest request, HttpServletResponse response, Model model) {
        String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
        log.trace("Logout was called with userTokenIdFromCookie={}. Redirecting to {}.", userTokenIdFromCookie, redirect_uri==null? ConstantValue.MYURI: redirect_uri );
        CookieManager.clearUserTokenCookies(request, response);
        return "redirect:" + ConstantValue.SSO_URI + "/logout?redirectURI=" + redirect_uri==null? ConstantValue.MYURI: redirect_uri;
    }

}
