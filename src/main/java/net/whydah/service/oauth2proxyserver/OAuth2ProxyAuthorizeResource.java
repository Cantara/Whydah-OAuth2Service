package net.whydah.service.oauth2proxyserver;

import net.whydah.service.authorizations.SSOUserSession;
import net.whydah.service.authorizations.UserAuthorization;
import net.whydah.service.authorizations.UserAuthorizationResource;
import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.service.clients.Client;
import net.whydah.service.clients.ClientService;
import net.whydah.service.errorhandling.AppException;
import net.whydah.sso.commands.adminapi.user.CommandUpdateUser;
import net.whydah.sso.commands.adminapi.user.CommandUpdateUserAggregate;
import net.whydah.sso.commands.adminapi.user.role.CommandAddUserRole;
import net.whydah.sso.commands.adminapi.user.role.CommandUpdateUserRole;
import net.whydah.sso.user.types.UserToken;
import net.whydah.util.CookieManager;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Date;
import java.util.List;

import static net.whydah.service.authorizations.UserAuthorizationService.DEVELOPMENT_USER_TOKEN_ID;
import static org.slf4j.LoggerFactory.getLogger;


@Path(OAuth2ProxyAuthorizeResource.OAUTH2AUTHORIZE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class OAuth2ProxyAuthorizeResource {
	public static final String OAUTH2AUTHORIZE_PATH = "/authorize";


	private static final Logger log = getLogger(OAuth2ProxyAuthorizeResource.class);
	private static final Logger auditLog = getLogger("auditLog");

	private final TokenService tokenService;
	private final UserAuthorizationService authorizationService;
	private final ClientService clientService;


	@Autowired
	public OAuth2ProxyAuthorizeResource(TokenService tokenService, UserAuthorizationService authorizationService, ClientService clientService) {
		this.tokenService = tokenService;
		this.authorizationService = authorizationService;
		this.clientService = clientService;
	}


	/**
	 * Ask the end user to authorize the client to access information described in scope.
	 * Implementation of https://tools.ietf.org/html/rfc6749#section-4.1.1
	 * @param response_type "code" or "token" REQUIRED
	 * @param scope OPTIONAl
	 * @param client_id REQUIRED
	 * @param redirect_uri OPTIONAL
	 * @param state value used by the client to maintain state between the request and callback. OPTIONAL
	 * @return HTTP 302 https://client.example.com/cb?code=SplxlOBeZQQYbYS6WxSbIA&state=xyz
	 * @throws MalformedURLException
	 * @throws AppException 
	 * 
	 * Extension for openid connect: https://darutk.medium.com/diagrams-of-all-the-openid-connect-flows-6968e3990660
	 */
	@GET
	public Response getOauth2ProxyServerController(
			@QueryParam("response_type") String response_type,

			//support response type
			//code
			//token
			//id_token
			//id_token token
			//code id_token
			//code token
			//code id_token token
			//none

			//Ref: https://darutk.medium.com/diagrams-of-all-the-openid-connect-flows-6968e3990660
			@QueryParam("scope") String scope,
			@QueryParam("client_id") String client_id,
			@QueryParam("redirect_uri") String redirect_uri,
			@QueryParam("state") String state,
			@QueryParam("nonce") String nonce,
			@DefaultValue("") @QueryParam("logged_in_users") String logged_in_users,
			@Context HttpServletRequest request, @Context HttpServletResponse httpServletResponse) throws MalformedURLException, AppException {
		log.debug("OAuth2ProxyAuthorizeResource - /authorize got response_type: {}" +
				"\n\tscope: {} \n\tclient_id: {} \n\tredirect_uri: {} \n\tstate: {} \n\tnonce: {}", response_type, scope, client_id, redirect_uri, state, nonce);

		if (scope == null) {
			//get default opendid connect scopes
			scope = "openid profile phone email";
		}

		SSOUserSession session = new SSOUserSession(scope, response_type, client_id, redirect_uri, state, nonce, logged_in_users, new Date());
		
		authorizationService.addSSOSession(session);
		
		String directUri = UriComponentsBuilder
				.fromUriString("." + UserAuthorizationResource.USER_PATH)
				.queryParam("oauth_session", session.getId()).build().toUriString();
		URI userAuthorization = URI.create(directUri);
		return Response.seeOther(userAuthorization).build();

	}

	@POST
	@Path("/acceptance")
	@Consumes("application/x-www-form-urlencoded")
	public Response userAcceptance(MultivaluedMap<String, String> formParams, @Context HttpServletRequest request) {
		log.trace("Acceptance sent. Values {}", formParams);

		String code = tokenService.buildCode();
		String client_id = formParams.getFirst("client_id");
		String accepted = formParams.getFirst("accepted");
		String redirect_uri = formParams.getFirst("redirect_uri");
		String response_type = formParams.getFirst("response_type");
		String state = formParams.getFirst("state");
		String nonce = formParams.getFirst("nonce");
		String scope = formParams.getFirst("scope");
		redirect_uri = clientService.getRedirectURI(client_id, redirect_uri).replaceFirst("/$", "");
		
		Client client = clientService.getClient(client_id);
		if (client == null) {
			URI userAgent_goto = URI.create(redirect_uri + "?error=client not found" + "&state=" + state);
			return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
		}
		String userTokenId = formParams.getFirst("usertoken_id");
		UserToken userToken = authorizationService.findUserTokenFromUserTokenId(userTokenId);
		if (userToken == null) {
			return authorizationService.toSSO(client_id, scope, response_type, state, nonce, redirect_uri, "");
		}

		if ("yes".equals(accepted.trim())) {
			auditLog.info("User accepted authorization. Code {}, FormParams {}", code, formParams);
			return forwardResponse(scope, code, client_id, redirect_uri, response_type, state, nonce, userToken);
		} else {
			//just returns to the current redirect_uri without a code
			return Response.status(Response.Status.FOUND).location(URI.create(redirect_uri + "?state=" + state + "&nonce=" + nonce)).build();
		}
	}
	
	
	@GET
	@Path("/acceptance")
	public Response userAcceptance(@Context HttpServletRequest request, 
			@QueryParam("client_id") String client_id, 
			@QueryParam("redirect_uri") String redirect_uri, 
			@QueryParam("response_type") String response_type,
			@QueryParam("scope") String scope,
			@QueryParam("state") String state, 
			@QueryParam("nonce") String nonce,
			@QueryParam("usertoken_id") String usertoken_id,
			@QueryParam("logged_in_users") String logged_in_users
			
			) {
		
		String code = tokenService.buildCode();
		
		redirect_uri = clientService.getRedirectURI(client_id, redirect_uri).replaceFirst("/$", "");
		
		Client client = clientService.getClient(client_id);
		if (client == null) {
			URI userAgent_goto = URI.create(redirect_uri + "?error=client not found" + "&state=" + state);
			return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
		}
		
		UserToken userToken = authorizationService.findUserTokenFromUserTokenId(usertoken_id);
		if (userToken == null) {
			return authorizationService.toSSO(client_id, scope, response_type, state, nonce, redirect_uri, logged_in_users);
		}

		auditLog.info("User implicitly accepted authorization. Code {}, client_id {}, redirect_uri {}, response_type {}, scope {}, state {}, nonce {}, usertoken_id{} ", code, client_id, redirect_uri, response_type, scope, state, nonce, usertoken_id);
		return forwardResponse(scope, code, client_id, redirect_uri, response_type, state, nonce, userToken);
		
	}
	
	
	public Response forwardResponse(String scope, String code, String client_id,
			String redirect_uri, String response_type, String state, String nonce,
			UserToken userToken) {
		List<String> scopes = authorizationService.buildScopes(scope);

		//save the scope to whydah roles
		authorizationService.saveScopesToWhydahRoles(userToken, scopes);
		
		//support response type
		//code
		//token
		//id_token
		//id_token token
		//code id_token
		//code token
		//code id_token token
		//none
		
		//Ref: https://darutk.medium.com/diagrams-of-all-the-openid-connect-flows-6968e3990660
		
		
		if(response_type.equalsIgnoreCase("code")) {
			clientService.addCode(code, nonce);
			UserAuthorization userAuthorization = new UserAuthorization(code, scopes, userToken.getUid().toString(), redirect_uri, userToken.getUserTokenId(), nonce);
			userAuthorization.setClientId(client_id);
			authorizationService.addAuthorization(userAuthorization);
			URI userAgent_goto = URI.create(redirect_uri + "?code=" + code + "&state=" + state + "&nonce=" + nonce);
			return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
		} else if(response_type.equalsIgnoreCase("token")) {
			try {

				JsonObject object = buildTokenAndgetJsonObject(client_id, redirect_uri, state, nonce, userToken, scopes);
				String access_token = object.getString("access_token");
				String refresh_token = object.getString("refresh_token");
				String token_type = object.getString("token_type");
				String expires_in = String.valueOf(object.getInt("expires_in"));
				URI userAgent_goto = URI.create(redirect_uri + "?access_token=" + access_token + "&refresh_token=" + refresh_token + "&token_type=" + token_type + "&expires_in=" + expires_in + "&state=" + state + "&nonce=" + nonce);
				return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
			} catch (AppException e) {
				URI userAgent_goto = URI.create(redirect_uri + "?error=" + e.getError() + "&state=" + state + "&nonce=" + nonce);
				return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
			}
			
		} else if(response_type.equalsIgnoreCase("id_token")) {
			try {

				JsonObject object = buildTokenAndgetJsonObject(client_id, redirect_uri, state, nonce, userToken, scopes);

				String id_token = object.getString("id_token");
				String token_type = object.getString("token_type");
				String expires_in = String.valueOf(object.getInt("expires_in"));

				URI userAgent_goto = URI.create(redirect_uri + "?id_token=" + id_token + "&token_type=" + token_type + "&expires_in=" + expires_in + "&state=" + state + "&nonce=" + nonce);
				return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
			} catch (AppException e) {
				URI userAgent_goto = URI.create(redirect_uri + "?error=" + e.getError() + "&state=" + state + "&nonce=" + nonce);
				return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
			}
		} else if(response_type.equalsIgnoreCase("id_token token")) {
			try {

				JsonObject object = buildTokenAndgetJsonObject(client_id, redirect_uri, state, nonce, userToken, scopes);
				String access_token = object.getString("access_token");
				String id_token = object.getString("id_token");
				String token_type = object.getString("token_type");
				String expires_in = String.valueOf(object.getInt("expires_in"));

				URI userAgent_goto = URI.create(redirect_uri + "?access_token=" + access_token + "&id_token=" + id_token + "&token_type=" + token_type + "&expires_in=" + expires_in + "&state=" + state + "&nonce=" + nonce);
				return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
			} catch (AppException e) {
				URI userAgent_goto = URI.create(redirect_uri + "?error=" + e.getError() + "&state=" + state);
				return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
			}
		}  else if(response_type.equalsIgnoreCase("code id_token")) {
			try {
				//issue a code
				UserAuthorization userAuthorization = new UserAuthorization(code, scopes, userToken.getUid().toString(), redirect_uri, userToken.getUserTokenId(), nonce);
				userAuthorization.setClientId(client_id);
				authorizationService.addAuthorization(userAuthorization);

				JsonObject object = buildTokenAndgetJsonObject(client_id, redirect_uri, state, nonce, userToken, scopes);

				String id_token = object.getString("id_token");
				String token_type = object.getString("token_type");
				String expires_in = String.valueOf(object.getInt("expires_in"));

				URI userAgent_goto = URI.create(redirect_uri + "?code=" + code + "&id_token=" + id_token + "&token_type=" + token_type + "&expires_in=" + expires_in + "&state=" + state + "&nonce=" + nonce);
				return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
			} catch (AppException e) {
				URI userAgent_goto = URI.create(redirect_uri + "?error=" + e.getError() + "&state=" + state);
				return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
			}
		} else if(response_type.equalsIgnoreCase("code token")) {
			try {
				//issue a code
				UserAuthorization userAuthorization = new UserAuthorization(code, scopes, userToken.getUid().toString(), redirect_uri, userToken.getUserTokenId(), nonce);
				userAuthorization.setClientId(client_id);
				authorizationService.addAuthorization(userAuthorization);

				JsonObject object = buildTokenAndgetJsonObject(client_id, redirect_uri, state, nonce, userToken, scopes);

				String access_token = object.getString("access_token");
				String token_type = object.getString("token_type");
				String expires_in = String.valueOf(object.getInt("expires_in"));

				URI userAgent_goto = URI.create(redirect_uri + "?code=" + code + "&access_token=" + access_token + "&token_type=" + token_type + "&expires_in=" + expires_in + "&state=" + state + "&nonce=" + nonce);
				return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
			} catch (AppException e) {
				URI userAgent_goto = URI.create(redirect_uri + "?error=" + e.getError() + "&state=" + state + "&nonce=" + nonce);
				return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
			}
		} else if(response_type.equalsIgnoreCase("code id_token token")) {
			try {
				//issue a code
				UserAuthorization userAuthorization = new UserAuthorization(code, scopes, userToken.getUid().toString(), redirect_uri, userToken.getUserTokenId(), nonce);
				userAuthorization.setClientId(client_id);
				authorizationService.addAuthorization(userAuthorization);

				JsonObject object = buildTokenAndgetJsonObject(client_id, redirect_uri, state, nonce, userToken, scopes);

				String id_token = object.getString("id_token");
				String access_token = object.getString("access_token");
				String token_type = object.getString("token_type");
				String expires_in = String.valueOf(object.getInt("expires_in"));

				URI userAgent_goto = URI.create(redirect_uri + "?code=" + code + "&id_token=" + id_token + "&access_token=" + access_token + "&token_type=" + token_type + "&expires_in=" + expires_in + "&state=" + state + "&nonce=" + nonce);
				return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
			} catch (AppException e) {
				URI userAgent_goto = URI.create(redirect_uri + "?error=" + e.getError() + "&state=" + state + "&nonce=" + nonce);
				return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
			}
		} else if(response_type.equalsIgnoreCase("none")) {
			URI userAgent_goto = URI.create(redirect_uri + "?state=" + state);
			return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
		} else {
			URI userAgent_goto = URI.create(redirect_uri + "?error=response_type not supported" + "&state=" + state + "&nonce=" + nonce);
			return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
		}
	}
	

	private JsonObject buildTokenAndgetJsonObject(String client_id, String redirect_uri, String state, String nonce, UserToken userToken, List<String> scopes) throws AppException {
		String jwt = tokenService.buildAccessToken(client_id, userToken, scopes, nonce);
		JsonReader jsonReader = Json.createReader(new StringReader(jwt));
		JsonObject object = jsonReader.readObject();
		jsonReader.close();
		return object;
	}


	

	protected String findWhydahUserId(MultivaluedMap<String, String> formParams, HttpServletRequest request) {
		String userTokenId = formParams.getFirst("usertoken_id");
		String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
		//Validate that usertoken has stayed the same. Ie user has not loged into another account.
		if (userTokenIdFromCookie == null) {
			userTokenIdFromCookie = DEVELOPMENT_USER_TOKEN_ID; //FIXME temporary
		}
		String whydahUserId = null;
		if (userTokenId != null && userTokenId.equals(userTokenIdFromCookie)) {
			UserToken userToken = authorizationService.findUserTokenFromUserTokenId(userTokenId);
			if (userToken != null) {
				whydahUserId = userToken.getUid();
			}
		}
		return whydahUserId;
	}

}

