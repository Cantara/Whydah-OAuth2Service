package net.whydah.service.oauth2proxyserver;

import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import io.jsonwebtoken.Claims;
import net.whydah.commands.config.ConfiguredValue;
import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.service.clients.ClientService;
import net.whydah.service.errorhandling.AppException;
import net.whydah.service.errorhandling.AppExceptionCode;
import net.whydah.util.CookieManager;
import net.whydah.util.FreeMarkerHelper;
import net.whydah.util.JwtUtils;

@Path(Oauth2ProxyLogoutResource.OAUTH2LOGOUT_PATH)
public class Oauth2ProxyLogoutResource {

	private static final Logger log = getLogger(Oauth2ProxyLogoutResource.class);

	public static final String OAUTH2LOGOUT_PATH = "/logout";
	
	private final UserAuthorizationService authorizationService;
	
	@Autowired
	public Oauth2ProxyLogoutResource(UserAuthorizationService userAuthorizationService) {
		this.authorizationService = userAuthorizationService;
	}

	@POST
	@Path("/confirm")
	@Consumes("application/x-www-form-urlencoded")
	public Response confirmLogout(MultivaluedMap<String, String> formParams, @Context HttpServletRequest request, @Context HttpServletResponse response) throws URISyntaxException, AppException {
		
		log.trace("Acceptance sent. Values {}", formParams);

		String usertoken_id = formParams.getFirst("usertoken_id");
		String accepted = formParams.getFirst("accepted");
		String redirect_uri = formParams.getFirst("redirect_uri");
		String state = formParams.getFirst("state");
	
		if ("yes".equals(accepted.trim())) {
			
			log.info("User accepted logout process. Usertoken id {}, redirect uri {}", usertoken_id, redirect_uri);
			
			authorizationService.releaseUserToken(usertoken_id);
			
			CookieManager.clearUserTokenCookies(request, response);
			
			return Response.status(Response.Status.FOUND).location(URI.create(redirect_uri + "?state=" + state)).build();
			
		} else {
			//just returns to the current redirect_uri without a code
			return Response.status(Response.Status.FOUND).location(URI.create(redirect_uri + "?state=" + state)).build();
		}		
		
	}
	

	@POST
	@Consumes("application/x-www-form-urlencoded")
	public Response logout(@Context HttpServletRequest request, @Context HttpServletResponse response, 
			MultivaluedMap<String, String> formParams
			) throws URISyntaxException, AppException {
		
		String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
		String post_logout_redirect_uri = formParams.getFirst("post_logout_redirect_uri");
		String id_token_hint = formParams.getFirst("id_token_hint");
		String state = formParams.getFirst("state");
		return processLogout(id_token_hint, post_logout_redirect_uri, state, userTokenId, request, response);
		
	}
	
	@GET
	public Response logout(@Context HttpServletRequest request, @Context HttpServletResponse response, 
			@QueryParam("id_token_hint") String id_token_hint,
			@QueryParam("post_logout_redirect_uri") String post_logout_redirect_uri,
			@QueryParam("state") String state
			) throws URISyntaxException, AppException {
		
		String userTokenId = CookieManager.getUserTokenIdFromCookie(request);
		return processLogout(id_token_hint, post_logout_redirect_uri, state, userTokenId, request, response);
		
	}

	private Response processLogout(String id_token_hint, String post_logout_redirect_uri, String state,
			String userTokenId, @Context HttpServletRequest request, @Context HttpServletResponse response) throws AppException {
		String redirectUri = null;
		
		if(post_logout_redirect_uri!=null) {
			if(id_token_hint!=null) {
				if(!JwtUtils.validateJwtToken(id_token_hint, RSAKeyFactory.getKey().getPublic())) {
					throw AppExceptionCode.MISC_RuntimeException_9994;
				}
				//this must be required
				redirectUri = post_logout_redirect_uri;	
			} 
		}
		
		if(id_token_hint!=null ) {
			Claims claims = JwtUtils.getClaims(id_token_hint, RSAKeyFactory.getKey().getPublic());
			if(claims == null) {
				throw AppExceptionCode.MISC_RuntimeException_9994;
			}
			userTokenId = claims.get("usertoken_id", String.class);
			if(redirectUri==null) {
				redirectUri = claims.get("app_url", String.class);
			}
		}
		
		if(userTokenId==null) {
			//just ok 
			return Response.ok().build();
		} else {
			if(redirectUri!=null) {
				//confirm the logout process
				Map<String, Object> model = new HashMap<String, Object>();
				model.put("logoURL", ConfiguredValue.getLogoUrl());
				model.put("usertoken_id", userTokenId);
				model.put("redirect_uri", redirectUri);
				model.put("state", state);	
				String body = FreeMarkerHelper.createBody("/LogoutConfirmation.ftl", model);
				return Response.ok(body).build();
			} else {
				authorizationService.releaseUserToken(userTokenId);
				
				CookieManager.clearUserTokenCookies(request, response);
				
				//just ok 
				return Response.ok().build();
			}
		}
	}
}
