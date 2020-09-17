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
import net.whydah.service.clients.Client;
import net.whydah.util.CookieManager;
import net.whydah.util.URLHelper;

@Path(Oauth2ProxyLogoutResource.OAUTH2LOGOUT_PATH)
public class Oauth2ProxyLogoutResource {

	private static final Logger log = getLogger(Oauth2ProxyLogoutResource.class);

	public static final String OAUTH2LOGOUT_PATH = "/logout";

	@GET
	public Response logout(@Context HttpServletRequest request, @Context HttpServletResponse response, 
			@QueryParam("redirect_uri") String redirect_uri,
			@QueryParam("logout_uri") String logout_uri,
			@QueryParam("client_id") String client_id
			) throws URISyntaxException {
		String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
		log.trace("Logout was called with userTokenIdFromCookie={}", userTokenIdFromCookie);
		CookieManager.clearUserTokenCookies(request, response);
		//TODO: do something with the client when it logs out
		String return_url = logout_uri!=null? logout_uri : (redirect_uri!=null? redirect_uri : null);
		if(return_url==null) {
			return Response.ok().build();
		} else {
			return Response.status(Response.Status.FOUND).location(URI.create(return_url)).build();
		}
	}
}
