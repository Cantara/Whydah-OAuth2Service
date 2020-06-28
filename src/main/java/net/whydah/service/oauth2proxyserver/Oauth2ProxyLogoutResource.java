package net.whydah.service.oauth2proxyserver;

import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import net.whydah.commands.config.ConstantValue;
import net.whydah.util.CookieManager;
import net.whydah.util.URLHelper;

@Path(Oauth2ProxyLogoutResource.OAUTH2LOGOUT_PATH)
public class Oauth2ProxyLogoutResource {

	private static final Logger log = getLogger(Oauth2ProxyLogoutResource.class);

	public static final String OAUTH2LOGOUT_PATH = "/logout";

	@GET
	public Response logout( @QueryParam("redirect_uri") String redirect_uri, @Context HttpServletRequest request, @Context HttpServletResponse response) throws URISyntaxException {
		String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
		log.trace("Logout was called with userTokenIdFromCookie={}. Redirecting to {}.", userTokenIdFromCookie, redirect_uri==null? "welcome": redirect_uri );
		CookieManager.clearUserTokenCookies(request, response);
		URI logout_redirect = URI.create(ConstantValue.SSO_URI + "/logout?redirectURI=" + (redirect_uri==null? "welcome": URLHelper.encode(redirect_uri)));
		return Response.status(Response.Status.MOVED_PERMANENTLY).location(logout_redirect).build();
	
		
	}
}
