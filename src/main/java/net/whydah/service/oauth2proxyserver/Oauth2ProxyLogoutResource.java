package net.whydah.service.oauth2proxyserver;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import net.whydah.commands.config.ConstantValues;
import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.service.clients.Client;
import net.whydah.service.clients.ClientService;
import net.whydah.service.errorhandling.AppException;
import net.whydah.util.CookieManager;
import net.whydah.util.FreeMarkerHelper;
import net.whydah.util.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@Path(Oauth2ProxyLogoutResource.OAUTH2LOGOUT_PATH)
@Component
public class Oauth2ProxyLogoutResource {

	private static final Logger log = LoggerFactory.getLogger(Oauth2ProxyLogoutResource.class);

	public static final String OAUTH2LOGOUT_PATH = "/logout";

	private final UserAuthorizationService authorizationService;

	private final ClientService clientService;

	@Autowired
	public Oauth2ProxyLogoutResource(UserAuthorizationService userAuthorizationService, ClientService clientService) {
		this.authorizationService = userAuthorizationService;
		this.clientService = clientService;
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

			if(state!=null) {

				return Response.status(Response.Status.FOUND).location(URI.create(redirect_uri + "?state=" + state)).build();
			} else {
				return Response.status(Response.Status.FOUND).location(URI.create(redirect_uri)).build();
			}
		} else {
			//just returns to the current redirect_uri without a code
			if(state!=null) {
				return Response.status(Response.Status.FOUND).location(URI.create(redirect_uri + "?state=" + state + "&canceled=true")).build();
			} else {
				return Response.status(Response.Status.FOUND).location(URI.create(redirect_uri + "?canceled=true")).build();
			}		
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

		String logout_uri = formParams.getFirst("logout_uri");
		String client_id = formParams.getFirst("client_id");

		if(client_id!=null) {
			return processOauth2Logout(client_id, logout_uri, userTokenId, request, response);
		} else {
			return processOpenIDConnectLogout(id_token_hint, post_logout_redirect_uri, state, userTokenId, request, response);
		}
	}

	@GET
	public Response logout(@Context HttpServletRequest request, @Context HttpServletResponse response, 
			@QueryParam("id_token_hint") String id_token_hint,
			@QueryParam("post_logout_redirect_uri") String post_logout_redirect_uri,
			@QueryParam("state") String state,

			//keep compatibility with Oauth2 
			@QueryParam("logout_uri") String logout_uri,
			@QueryParam("client_id") String client_id
			) throws URISyntaxException, AppException {

		String userTokenId = CookieManager.getUserTokenIdFromCookie(request);

		if(client_id!=null) {
			return processOauth2Logout(client_id, logout_uri, userTokenId, request, response);
		} else {
			return processOpenIDConnectLogout(id_token_hint, post_logout_redirect_uri, state, userTokenId, request, response);
		}
	}

	private Response processOauth2Logout(String client_id, String logout_uri, String userTokenId, HttpServletRequest request, HttpServletResponse response) {


		authorizationService.releaseUserToken(userTokenId);

		CookieManager.clearUserTokenCookies(request, response);

		String redirect_uri = null;
		if(logout_uri!=null) {
			redirect_uri = logout_uri;
		} else {
			Client client = clientService.getClient(client_id);
			if(client.getRedirectUrl()!=null) {
				redirect_uri = client.getRedirectUrl();
			} else if (client.getApplicationUrl()!=null) {
				redirect_uri = client.getApplicationUrl();
			}
		}

		if(redirect_uri==null) {
			return Response.ok().build();
		} else {
			return Response.status(Response.Status.FOUND).location(URI.create(redirect_uri)).build();
		}

	}

	private Response processOpenIDConnectLogout(String id_token_hint, String post_logout_redirect_uri, String state,
			String userTokenId, @Context HttpServletRequest request, @Context HttpServletResponse response) throws AppException {
		String redirectUri = null;
		String username = "";
		if(post_logout_redirect_uri!=null) {
			if(id_token_hint!=null) {
				redirectUri = post_logout_redirect_uri;	
			}
		}

		if(id_token_hint!=null ) {

			Claims claims = JwtUtils.parseRSAJwtToken(id_token_hint);
			if(claims != null) {

				userTokenId = claims.get("usertoken_id", String.class);
				username = claims.get(Claims.SUBJECT, String.class);

				if(redirectUri==null) {
					try {
						Client client  = clientService.getClient(claims.get(Claims.AUDIENCE, String.class));
						if(client.getRedirectUrl()!=null) {
							redirectUri = client.getRedirectUrl();
						} else if (client.getApplicationUrl()!=null) {
							redirectUri = client.getApplicationUrl();
						}
					} catch(Exception ex) {
						ex.printStackTrace();
					}
				}
			}

		}

		if(userTokenId==null) {
			if(redirectUri!=null) {
				if(state!=null) {
					return Response.status(Response.Status.FOUND).location(URI.create(redirectUri+ "?state=" + state)).build();	
				} else {
					return Response.status(Response.Status.FOUND).location(URI.create(redirectUri)).build();	
				}

			} else {
				//just ok 
				return Response.ok().build();
			}
		} else {
			if(redirectUri!=null) {
				if(ConstantValues.LOGOUT_CONFIRM_ENABLED) {
					//confirm the logout process
					Map<String, Object> model = new HashMap<String, Object>();
					model.put("logoURL", ConstantValues.getLogoUrl());
					model.put("usertoken_id", userTokenId);
					model.put("redirect_uri", redirectUri);
					model.put("state", state);	
					model.put("username", username);
					String body = FreeMarkerHelper.createBody("/LogoutConfirmation.ftl", model);
					return Response.ok(body).build();
				} else {

					authorizationService.releaseUserToken(userTokenId);

					CookieManager.clearUserTokenCookies(request, response);

					if(state!=null) {
						return Response.status(Response.Status.FOUND).location(URI.create(redirectUri + "?state=" + state)).build();
					} else {
						return Response.status(Response.Status.FOUND).location(URI.create(redirectUri)).build();
					}

				}
			} else {
				authorizationService.releaseUserToken(userTokenId);
				CookieManager.clearUserTokenCookies(request, response);
				//just ok 
				return Response.ok().build();
			}
		}
	}
}
