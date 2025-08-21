package net.whydah.service.oauth2proxyserver;

import static net.whydah.service.authorizations.UserAuthorizationService.DEVELOPMENT_USER_TOKEN_ID;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import net.whydah.commands.config.ConstantValues;
import net.whydah.service.authorizations.OAuthenticationSession;
import net.whydah.service.authorizations.UserAuthorizationResource;
import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.service.authorizations.UserAuthorizationSession;
import net.whydah.service.clients.Client;
import net.whydah.service.clients.ClientService;
import net.whydah.service.errorhandling.AppException;
import net.whydah.sso.user.types.UserToken;
import net.whydah.util.CookieManager;
import net.whydah.util.FreeMarkerHelper;
import net.whydah.util.UriBuilder;

@Path(OAuth2ProxyAuthorizeResource.OAUTH2AUTHORIZE_PATH)
public class OAuth2ProxyAuthorizeResource {
	public static final String OAUTH2AUTHORIZE_PATH = "/authorize";

	private static final Logger log = LoggerFactory.getLogger(OAuth2ProxyAuthorizeResource.class);

	private final TokenService tokenService;
	private final UserAuthorizationService authorizationService;
	private final ClientService clientService;

	@Inject
	public OAuth2ProxyAuthorizeResource(TokenService tokenService,
										UserAuthorizationService authorizationService,
										ClientService clientService) {
		this.tokenService = tokenService;
		this.authorizationService = authorizationService;
		this.clientService = clientService;
		log.info("OAuth2ProxyAuthorizeResource created with constructor injection");
	}

	/**
	 * Helper method to safely decode URL parameters
	 */
	private String safeUrlDecode(String url) {
		if (url != null && url.contains("%")) {
			try {
				return URLDecoder.decode(url, StandardCharsets.UTF_8);
			} catch (Exception e) {
				log.warn("Failed to decode URL: {}", url, e);
				return url;
			}
		}
		return url;
	}

	/**
	 * Helper method to safely encode URL parameters
	 */
	private String safeUrlEncode(String value) {
		if (value == null) return "";
		try {
			return URLEncoder.encode(value, StandardCharsets.UTF_8);
		} catch (Exception e) {
			log.warn("Failed to encode value: {}", value, e);
			return value;
		}
	}

	/**
	 * Ask the end user to authorize the client to access information described in scope.
	 * Implementation of https://tools.ietf.org/html/rfc6749#section-4.1.1
	 */
	@GET
	public Response getOauth2ProxyServerController(
			@QueryParam("response_mode") String response_mode,
			@QueryParam("response_type") String response_type,
			@QueryParam("scope") String scope,
			@QueryParam("client_id") String client_id,
			@QueryParam("redirect_uri") String redirect_uri,
			@QueryParam("state") String state,
			@QueryParam("nonce") String nonce,
			@QueryParam("code_challenge") String code_challenge,
			@DefaultValue("plain") @QueryParam("code_challenge_method") String code_challenge_method,
			@DefaultValue("") @QueryParam("logged_in_users") String logged_in_users,
			@DefaultValue("whydah") @QueryParam("referer_channel") String referer_channel,
			@Context HttpServletRequest request, 
			@Context HttpServletResponse httpServletResponse) throws MalformedURLException, AppException {

		// Decode redirect_uri immediately
		redirect_uri = safeUrlDecode(redirect_uri);

		log.debug("""
                OAuth2ProxyAuthorizeResource - /authorize got response_type: {}
                	scope: {} 
                	client_id: {} 
                	redirect_uri: {} 
                	state: {} 
                	nonce: {}
                """, response_type, scope, client_id, redirect_uri, state, nonce);

		// Validate required parameters
		if (client_id == null || client_id.trim().isEmpty()) {
			if (redirect_uri != null) {
				return sendError(redirect_uri, "query", state, nonce, "invalid_client", "client_id is required");
			} else {
				return Response.status(Response.Status.BAD_REQUEST)
					.entity("{\"error\":\"invalid_client\",\"error_description\":\"client_id is required\"}")
					.build();
			}
		}

		if (response_type == null || response_type.trim().isEmpty()) {
			return sendError(redirect_uri, "query", state, nonce, "invalid_request", "response_type is required");
		}

		if (scope == null) {
			scope = "openid profile phone email";
		}

		if(response_mode == null) {
			if(response_type.equalsIgnoreCase("code")) {
				response_mode = "query";
			} else {
				response_mode = "fragment";
			}
		}

		OAuthenticationSession session = new OAuthenticationSession(scope, response_type, response_mode, 
				client_id, redirect_uri, state, nonce, code_challenge, code_challenge_method, 
				logged_in_users, referer_channel, new Date());

		authorizationService.addSSOSession(session);

		String directUri = UriBuilder
			    .fromUriString(ConstantValues.MYURI)
			    .path(UserAuthorizationResource.USER_PATH)
			    .queryParam("oauth_session", session.getId())
			    .toUriString();

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
		String redirect_uri = safeUrlDecode(formParams.getFirst("redirect_uri"));
		String response_type = formParams.getFirst("response_type");
		String response_mode = formParams.getFirst("response_mode");
		String state = formParams.getFirst("state");
		String nonce = formParams.getFirst("nonce");
		String scope = formParams.getFirst("scope");
		String code_challenge = formParams.getFirst("code_challenge");
		String code_challenge_method = formParams.getFirst("code_challenge_method");
		String referer_channel = formParams.getFirst("referer_channel");

		try {
			redirect_uri = clientService.getRedirectURI(client_id, redirect_uri);
			if (redirect_uri != null) {
				redirect_uri = redirect_uri.replaceFirst("/$", "");
			}
		} catch (Exception e) {
			log.error("Error getting redirect URI for client {}: {}", client_id, e.getMessage());
			return sendError(redirect_uri, response_mode != null ? response_mode : "query", state, nonce, 
					"invalid_client", "Invalid client configuration");
		}

		Client client = clientService.getClient(client_id);
		if (client == null) {
			return sendError(redirect_uri, response_mode != null ? response_mode : "query", state, nonce, 
					"invalid_client", "Client not found");
		}

		String userTokenId = formParams.getFirst("usertoken_id");
		UserToken userToken = authorizationService.findUserTokenFromUserTokenId(userTokenId);
		if (userToken == null) {
			return authorizationService.toSSO(client_id, scope, response_type, response_mode, state, nonce, 
					redirect_uri, "", referer_channel, code_challenge, code_challenge_method);
		}

		if ("yes".equals(accepted.trim())) {
			log.info("User accepted authorization. Code {}, FormParams {}", code, formParams);
			return forwardResponse(scope, code, client_id, redirect_uri, response_type, response_mode, 
					state, nonce, userToken, referer_channel, code_challenge, code_challenge_method);
		} else {
			return sendError(redirect_uri, response_mode != null ? response_mode : "query", state, nonce, 
					"access_denied", "User denied the request");
		}
	}

	@GET
	@Path("/acceptance")
	public Response userAcceptance(@Context HttpServletRequest request,
								   @QueryParam("client_id") String client_id,
								   @QueryParam("redirect_uri") String redirect_uri,
								   @QueryParam("response_type") String response_type,
								   @QueryParam("response_mode") String response_mode,
								   @QueryParam("scope") String scope,
								   @QueryParam("state") String state,
								   @QueryParam("nonce") String nonce,
								   @QueryParam("usertoken_id") String usertoken_id,
								   @QueryParam("logged_in_users") String logged_in_users,
								   @QueryParam("referer_channel") String referer_channel,
								   @QueryParam("code_challenge") String code_challenge,
								   @QueryParam("code_challenge_method") String code_challenge_method
	) {

		// Decode redirect_uri immediately
		redirect_uri = safeUrlDecode(redirect_uri);

		String code = tokenService.buildCode();

		try {
			redirect_uri = clientService.getRedirectURI(client_id, redirect_uri);
			if (redirect_uri != null) {
				redirect_uri = redirect_uri.replaceFirst("/$", "");
			}
		} catch (Exception e) {
			log.error("Error getting redirect URI for client {}: {}", client_id, e.getMessage());
			return sendError(redirect_uri, response_mode != null ? response_mode : "query", state, nonce, 
					"invalid_client", "Invalid client configuration");
		}

		Client client = clientService.getClient(client_id);
		if (client == null) {
			return sendError(redirect_uri, response_mode != null ? response_mode : "query", state, nonce, 
					"invalid_client", "Client not found");
		}

		UserToken userToken = authorizationService.findUserTokenFromUserTokenId(usertoken_id);
		if (userToken == null) {
			return authorizationService.toSSO(client_id, scope, response_type, response_mode, state, nonce, 
					redirect_uri, logged_in_users, referer_channel, code_challenge, code_challenge_method);
		}

		log.info("User implicitly accepted authorization. Code {}, client_id {}, redirect_uri {}, response_type {}, scope {}, state {}, nonce {}, usertoken_id{} ", 
				code, client_id, redirect_uri, response_type, scope, state, nonce, usertoken_id);
		return forwardResponse(scope, code, client_id, redirect_uri, response_type, response_mode, 
				state, nonce, userToken, referer_channel, code_challenge, code_challenge_method);
	}

	public Response forwardResponse(String scope, String code, String client_id,
									String redirect_uri, String response_type, String response_mode, String state, String nonce,
									UserToken userToken, String referer_channel, String code_challenge, String code_challenge_method) {

		List<String> scopes = authorizationService.buildScopes(scope);
		authorizationService.saveScopesToWhydahRoles(userToken, scopes);

		// Ensure redirect_uri is decoded
		redirect_uri = safeUrlDecode(redirect_uri);

		if(response_type.equalsIgnoreCase("code")) {
			return handleCodeFlow(scope, code, client_id, redirect_uri, state, nonce, userToken, 
					referer_channel, code_challenge, code_challenge_method, scopes);
			
		} else if(response_type.equalsIgnoreCase("token")) {
			return handleTokenFlow(client_id, redirect_uri, response_mode, state, nonce, userToken, 
					referer_channel, scopes, null);
			
		} else if(response_type.equalsIgnoreCase("id_token")) {
			return handleIdTokenFlow(client_id, redirect_uri, response_mode, state, nonce, userToken, 
					referer_channel, scopes, null);
			
		} else if(response_type.equalsIgnoreCase("id_token token") || response_type.equalsIgnoreCase("token id_token")) {
			return handleIdTokenTokenFlow(client_id, redirect_uri, response_mode, state, nonce, userToken, 
					referer_channel, scopes, code);
			
		} else if(response_type.equalsIgnoreCase("code id_token") || response_type.equalsIgnoreCase("id_token code")) {
			return handleCodeIdTokenFlow(scope, code, client_id, redirect_uri, response_mode, state, nonce, 
					userToken, referer_channel, code_challenge, code_challenge_method, scopes);
			
		} else if(response_type.equalsIgnoreCase("code token") || response_type.equalsIgnoreCase("token code")) {
			return handleCodeTokenFlow(scope, code, client_id, redirect_uri, response_mode, state, nonce, 
					userToken, referer_channel, code_challenge, code_challenge_method, scopes);
			
		} else if (Arrays.asList("code", "id_token", "token").containsAll(Arrays.asList(response_type.split("\\s+")))) {
			return handleCodeIdTokenTokenFlow(scope, code, client_id, redirect_uri, response_mode, state, nonce, 
					userToken, referer_channel, code_challenge, code_challenge_method, scopes);
			
		} else if(response_type.equalsIgnoreCase("none")) {
			return handleNoneFlow(redirect_uri, response_mode, state, nonce, referer_channel);
			
		} else {
			return sendError(redirect_uri, response_mode, state, nonce, "unsupported_response_type", 
					"Response type not supported: " + response_type);
		}
	}

	private Response handleCodeFlow(String scope, String code, String client_id, String redirect_uri,
									String state, String nonce, UserToken userToken, String referer_channel,
									String code_challenge, String code_challenge_method, List<String> scopes) {
		clientService.addCode(code, nonce);
		UserAuthorizationSession userAuthorization = new UserAuthorizationSession(code, scopes, 
				userToken.getUid().toString(), redirect_uri, userToken.getUserTokenId(), nonce, 
				code_challenge, code_challenge_method);
		userAuthorization.setClientId(client_id);
		authorizationService.addAuthorization(userAuthorization);
		
		StringBuilder urlBuilder = new StringBuilder(redirect_uri);
		urlBuilder.append(redirect_uri.contains("?") ? "&" : "?");
		urlBuilder.append("code=").append(safeUrlEncode(code));
		if (state != null) urlBuilder.append("&state=").append(safeUrlEncode(state));
		if (nonce != null) urlBuilder.append("&nonce=").append(safeUrlEncode(nonce));
		if (referer_channel != null) urlBuilder.append("&referer_channel=").append(safeUrlEncode(referer_channel));
		if (code_challenge != null && code_challenge.length() > 0) {
			urlBuilder.append("&code_challenge=").append(safeUrlEncode(code_challenge));
			urlBuilder.append("&code_challenge_method=").append(safeUrlEncode(code_challenge_method));
		}
		
		URI userAgent_goto = URI.create(urlBuilder.toString());
		return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
	}

	private Response handleTokenFlow(String client_id, String redirect_uri, String response_mode,
									String state, String nonce, UserToken userToken, String referer_channel,
									List<String> scopes, String code) {
		try {
			JsonObject object = buildTokenAndgetJsonObject(client_id, redirect_uri, state, nonce, userToken, scopes, code);
			String access_token = object.getString("access_token");
			String refresh_token = object.getString("refresh_token");
			String token_type = object.getString("token_type");
			String expires_in = String.valueOf(object.getInt("expires_in"));

			if("form_post".equalsIgnoreCase(response_mode)) {
				Map<String, Object> model = new HashMap<>();
				model.put("access_token", access_token);
				model.put("refresh_token", refresh_token);
				model.put("token_type", token_type);
				model.put("expires_in", expires_in);
				model.put("redirect_uri", redirect_uri);
				model.put("state", state);
				model.put("nonce", nonce);
				model.put("referer_channel", referer_channel);
				return Response.ok(FreeMarkerHelper.createBody("/ImplicitAndHybridFlowResponse.ftl", model)).build();
			} else {
				StringBuilder fragmentBuilder = new StringBuilder(redirect_uri);
				fragmentBuilder.append("#access_token=").append(access_token);
				fragmentBuilder.append("&refresh_token=").append(refresh_token);
				fragmentBuilder.append("&token_type=").append(token_type);
				fragmentBuilder.append("&expires_in=").append(expires_in);
				if (state != null) fragmentBuilder.append("&state=").append(state);
				if (nonce != null) fragmentBuilder.append("&nonce=").append(nonce);
				if (referer_channel != null) fragmentBuilder.append("&referer_channel=").append(referer_channel);
				
				URI userAgent_goto = URI.create(fragmentBuilder.toString());
				return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
			}
		} catch (AppException e) {
			log.error("Error building token response", e);
			return sendError(redirect_uri, response_mode, state, nonce, "server_error", e.getError());
		} catch (Exception e) {
			log.error("Unexpected error building token response", e);
			return sendError(redirect_uri, response_mode, state, nonce, "server_error", "Internal server error");
		}
	}

	private Response handleIdTokenFlow(String client_id, String redirect_uri, String response_mode,
									  String state, String nonce, UserToken userToken, String referer_channel,
									  List<String> scopes, String code) {
		try {
			JsonObject object = buildTokenAndgetJsonObject(client_id, redirect_uri, state, nonce, userToken, scopes, code);
			String refresh_token = object.getString("refresh_token");
			String id_token = object.getString("id_token");
			String token_type = object.getString("token_type");
			String expires_in = String.valueOf(object.getInt("expires_in"));

			if("form_post".equalsIgnoreCase(response_mode)) {
				Map<String, Object> model = new HashMap<>();
				model.put("id_token", id_token);
				model.put("refresh_token", refresh_token);
				model.put("token_type", token_type);
				model.put("redirect_uri", redirect_uri);
				model.put("expires_in", expires_in);
				model.put("state", state);
				model.put("nonce", nonce);
				model.put("referer_channel", referer_channel);
				return Response.ok(FreeMarkerHelper.createBody("/ImplicitAndHybridFlowResponse.ftl", model)).build();
			} else {
				StringBuilder fragmentBuilder = new StringBuilder(redirect_uri);
				fragmentBuilder.append("#id_token=").append(id_token);
				fragmentBuilder.append("&refresh_token=").append(refresh_token);
				fragmentBuilder.append("&token_type=").append(token_type);
				fragmentBuilder.append("&expires_in=").append(expires_in);
				if (state != null) fragmentBuilder.append("&state=").append(state);
				if (nonce != null) fragmentBuilder.append("&nonce=").append(nonce);
				if (referer_channel != null) fragmentBuilder.append("&referer_channel=").append(referer_channel);
				
				URI userAgent_goto = URI.create(fragmentBuilder.toString());
				return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
			}
		} catch (AppException e) {
			log.error("Error building id_token response", e);
			return sendError(redirect_uri, response_mode, state, nonce, "server_error", e.getError());
		} catch (Exception e) {
			log.error("Unexpected error building id_token response", e);
			return sendError(redirect_uri, response_mode, state, nonce, "server_error", "Internal server error");
		}
	}

	private Response handleIdTokenTokenFlow(String client_id, String redirect_uri, String response_mode,
										   String state, String nonce, UserToken userToken, String referer_channel,
										   List<String> scopes, String code) {
		try {
			JsonObject object = buildTokenAndgetJsonObject(client_id, redirect_uri, state, nonce, userToken, scopes, code);
			String refresh_token = object.getString("refresh_token");
			String access_token = object.getString("access_token");
			String id_token = object.getString("id_token");
			String token_type = object.getString("token_type");
			String expires_in = String.valueOf(object.getInt("expires_in"));

			if("form_post".equalsIgnoreCase(response_mode)) {
				Map<String, Object> model = new HashMap<>();
				model.put("id_token", id_token);
				model.put("access_token", access_token);
				model.put("refresh_token", refresh_token);
				model.put("token_type", token_type);
				model.put("expires_in", expires_in);
				model.put("redirect_uri", redirect_uri);
				model.put("state", state);
				model.put("nonce", nonce);
				model.put("referer_channel", referer_channel);
				return Response.ok(FreeMarkerHelper.createBody("/ImplicitAndHybridFlowResponse.ftl", model)).build();
			} else {
				StringBuilder fragmentBuilder = new StringBuilder(redirect_uri);
				fragmentBuilder.append("#access_token=").append(access_token);
				fragmentBuilder.append("&id_token=").append(id_token);
				fragmentBuilder.append("&refresh_token=").append(refresh_token);
				fragmentBuilder.append("&token_type=").append(token_type);
				fragmentBuilder.append("&expires_in=").append(expires_in);
				if (state != null) fragmentBuilder.append("&state=").append(state);
				if (nonce != null) fragmentBuilder.append("&nonce=").append(nonce);
				if (referer_channel != null) fragmentBuilder.append("&referer_channel=").append(referer_channel);
				
				URI userAgent_goto = URI.create(fragmentBuilder.toString());
				return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
			}
		} catch (AppException e) {
			log.error("Error building id_token token response", e);
			return sendError(redirect_uri, response_mode, state, nonce, "server_error", e.getError());
		} catch (Exception e) {
			log.error("Unexpected error building id_token token response", e);
			return sendError(redirect_uri, response_mode, state, nonce, "server_error", "Internal server error");
		}
	}

	private Response handleCodeIdTokenFlow(String scope, String code, String client_id, String redirect_uri,
										  String response_mode, String state, String nonce, UserToken userToken,
										  String referer_channel, String code_challenge, String code_challenge_method,
										  List<String> scopes) {
		try {
			clientService.addCode(code, nonce);
			UserAuthorizationSession userAuthorization = new UserAuthorizationSession(code, scopes, 
					userToken.getUid().toString(), redirect_uri, userToken.getUserTokenId(), nonce, 
					code_challenge, code_challenge_method);
			userAuthorization.setClientId(client_id);
			authorizationService.addAuthorization(userAuthorization);

			JsonObject object = buildTokenAndgetJsonObject(client_id, redirect_uri, state, nonce, userToken, scopes, code);
			String refresh_token = object.getString("refresh_token");
			String id_token = object.getString("id_token");
			String token_type = object.getString("token_type");
			String expires_in = String.valueOf(object.getInt("expires_in"));

			if("form_post".equalsIgnoreCase(response_mode)) {
				Map<String, Object> model = new HashMap<>();
				model.put("id_token", id_token);
				model.put("refresh_token", refresh_token);
				model.put("code", code);
				model.put("token_type", token_type);
				model.put("expires_in", expires_in);
				model.put("redirect_uri", redirect_uri);
				model.put("state", state);
				model.put("nonce", nonce);
				model.put("referer_channel", referer_channel);
				if (code_challenge != null) {
					model.put("code_challenge", code_challenge);
					model.put("code_challenge_method", code_challenge_method);
				}
				return Response.ok(FreeMarkerHelper.createBody("/ImplicitAndHybridFlowResponse.ftl", model)).build();
			} else {
				StringBuilder fragmentBuilder = new StringBuilder(redirect_uri);
				fragmentBuilder.append("#code=").append(code);
				fragmentBuilder.append("&id_token=").append(id_token);
				fragmentBuilder.append("&refresh_token=").append(refresh_token);
				fragmentBuilder.append("&token_type=").append(token_type);
				fragmentBuilder.append("&expires_in=").append(expires_in);
				if (state != null) fragmentBuilder.append("&state=").append(state);
				if (nonce != null) fragmentBuilder.append("&nonce=").append(nonce);
				if (referer_channel != null) fragmentBuilder.append("&referer_channel=").append(referer_channel);
				if (code_challenge != null && code_challenge.length() > 0) {
					fragmentBuilder.append("&code_challenge=").append(code_challenge);
					fragmentBuilder.append("&code_challenge_method=").append(code_challenge_method);
				}
				
				URI userAgent_goto = URI.create(fragmentBuilder.toString());
				return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
			}
		} catch (AppException e) {
			log.error("Error building code id_token response", e);
			return sendError(redirect_uri, response_mode, state, nonce, "server_error", e.getError());
		} catch (Exception e) {
			log.error("Unexpected error building code id_token response", e);
			return sendError(redirect_uri, response_mode, state, nonce, "server_error", "Internal server error");
		}
	}

	private Response handleCodeTokenFlow(String scope, String code, String client_id, String redirect_uri,
										String response_mode, String state, String nonce, UserToken userToken,
										String referer_channel, String code_challenge, String code_challenge_method,
										List<String> scopes) {
		try {
			clientService.addCode(code, nonce);
			UserAuthorizationSession userAuthorization = new UserAuthorizationSession(code, scopes, 
					userToken.getUid().toString(), redirect_uri, userToken.getUserTokenId(), nonce, 
					code_challenge, code_challenge_method);
			userAuthorization.setClientId(client_id);
			authorizationService.addAuthorization(userAuthorization);

			JsonObject object = buildTokenAndgetJsonObject(client_id, redirect_uri, state, nonce, userToken, scopes, code);
			String refresh_token = object.getString("refresh_token");
			String access_token = object.getString("access_token");
			String token_type = object.getString("token_type");
			String expires_in = String.valueOf(object.getInt("expires_in"));

			if("form_post".equalsIgnoreCase(response_mode)) {
				Map<String, Object> model = new HashMap<>();
				model.put("access_token", access_token);
				model.put("refresh_token", refresh_token);
				model.put("code", code);
				model.put("token_type", token_type);
				model.put("expires_in", expires_in);
				model.put("redirect_uri", redirect_uri);
				model.put("state", state);
				model.put("nonce", nonce);
				model.put("referer_channel", referer_channel);
				if (code_challenge != null) {
					model.put("code_challenge", code_challenge);
					model.put("code_challenge_method", code_challenge_method);
				}
				return Response.ok(FreeMarkerHelper.createBody("/ImplicitAndHybridFlowResponse.ftl", model)).build();
			} else {
				StringBuilder fragmentBuilder = new StringBuilder(redirect_uri);
				fragmentBuilder.append("#code=").append(code);
				fragmentBuilder.append("&access_token=").append(access_token);
				fragmentBuilder.append("&refresh_token=").append(refresh_token);
				fragmentBuilder.append("&token_type=").append(token_type);
				fragmentBuilder.append("&expires_in=").append(expires_in);
				if (state != null) fragmentBuilder.append("&state=").append(state);
				if (nonce != null) fragmentBuilder.append("&nonce=").append(nonce);
				if (referer_channel != null) fragmentBuilder.append("&referer_channel=").append(referer_channel);
				if (code_challenge != null && code_challenge.length() > 0) {
					fragmentBuilder.append("&code_challenge=").append(code_challenge);
					fragmentBuilder.append("&code_challenge_method=").append(code_challenge_method);
				}
				
				URI userAgent_goto = URI.create(fragmentBuilder.toString());
				return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
			}
		} catch (AppException e) {
			log.error("Error building code token response", e);
			return sendError(redirect_uri, response_mode, state, nonce, "server_error", e.getError());
		} catch (Exception e) {
			log.error("Unexpected error building code token response", e);
			return sendError(redirect_uri, response_mode, state, nonce, "server_error", "Internal server error");
		}
	}

	private Response handleCodeIdTokenTokenFlow(String scope, String code, String client_id, String redirect_uri,
											   String response_mode, String state, String nonce, UserToken userToken,
											   String referer_channel, String code_challenge, String code_challenge_method,
											   List<String> scopes) {
		try {
			clientService.addCode(code, nonce);
			UserAuthorizationSession userAuthorization = new UserAuthorizationSession(code, scopes, 
					userToken.getUid().toString(), redirect_uri, userToken.getUserTokenId(), nonce, 
					code_challenge, code_challenge_method);
			userAuthorization.setClientId(client_id);
			authorizationService.addAuthorization(userAuthorization);

			JsonObject object = buildTokenAndgetJsonObject(client_id, redirect_uri, state, nonce, userToken, scopes, code);
			String id_token = object.getString("id_token");
			String refresh_token = object.getString("refresh_token");
			String access_token = object.getString("access_token");
			String token_type = object.getString("token_type");
			String expires_in = String.valueOf(object.getInt("expires_in"));

			if("form_post".equalsIgnoreCase(response_mode)) {
				Map<String, Object> model = new HashMap<>();
				model.put("access_token", access_token);
				model.put("id_token", id_token);
				model.put("refresh_token", refresh_token);
				model.put("code", code);
				model.put("token_type", token_type);
				model.put("expires_in", expires_in);
				model.put("redirect_uri", redirect_uri);
				model.put("state", state);
				model.put("nonce", nonce);
				model.put("referer_channel", referer_channel);
				if (code_challenge != null) {
					model.put("code_challenge", code_challenge);
					model.put("code_challenge_method", code_challenge_method);
				}
				return Response.ok(FreeMarkerHelper.createBody("/ImplicitAndHybridFlowResponse.ftl", model)).build();
			} else {
				StringBuilder fragmentBuilder = new StringBuilder(redirect_uri);
				fragmentBuilder.append("#code=").append(code);
				fragmentBuilder.append("&id_token=").append(id_token);
				fragmentBuilder.append("&access_token=").append(access_token);
				fragmentBuilder.append("&refresh_token=").append(refresh_token);
				fragmentBuilder.append("&token_type=").append(token_type);
				fragmentBuilder.append("&expires_in=").append(expires_in);
				if (state != null) fragmentBuilder.append("&state=").append(state);
				if (nonce != null) fragmentBuilder.append("&nonce=").append(nonce);
				if (referer_channel != null) fragmentBuilder.append("&referer_channel=").append(referer_channel);
				if (code_challenge != null && code_challenge.length() > 0) {
					fragmentBuilder.append("&code_challenge=").append(code_challenge);
					fragmentBuilder.append("&code_challenge_method=").append(code_challenge_method);
				}
				
				URI userAgent_goto = URI.create(fragmentBuilder.toString());
				return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
			}
		} catch (AppException e) {
			log.error("Error building code id_token token response", e);
			return sendError(redirect_uri, response_mode, state, nonce, "server_error", e.getError());
		} catch (Exception e) {
			log.error("Unexpected error building code id_token token response", e);
			return sendError(redirect_uri, response_mode, state, nonce, "server_error", "Internal server error");
		}
	}

	private Response handleNoneFlow(String redirect_uri, String response_mode, String state, String nonce, String referer_channel) {
		if("form_post".equalsIgnoreCase(response_mode)) {
			Map<String, Object> model = new HashMap<>();
			model.put("state", state);
			model.put("nonce", nonce);
			model.put("redirect_uri", redirect_uri);
			model.put("referer_channel", referer_channel);
			Viewable gui = new Viewable("/ImplicitAndHybridFlowResponse.ftl", model);
			return Response.ok(gui).build();
		} else if("fragment".equalsIgnoreCase(response_mode)) {
			StringBuilder fragmentBuilder = new StringBuilder(redirect_uri);
			fragmentBuilder.append("#");
			if (state != null) fragmentBuilder.append("state=").append(state);
			if (nonce != null) {
				if (state != null) fragmentBuilder.append("&");
				fragmentBuilder.append("nonce=").append(nonce);
			}
			if (referer_channel != null) {
				if (state != null || nonce != null) fragmentBuilder.append("&");
				fragmentBuilder.append("referer_channel=").append(referer_channel);
			}
			
			URI userAgent_goto = URI.create(fragmentBuilder.toString());
			return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
		} else {
			StringBuilder queryBuilder = new StringBuilder(redirect_uri);
			queryBuilder.append(redirect_uri.contains("?") ? "&" : "?");
			if (state != null) queryBuilder.append("state=").append(safeUrlEncode(state));
			if (nonce != null) {
				if (state != null) queryBuilder.append("&");
				queryBuilder.append("nonce=").append(safeUrlEncode(nonce));
			}
			if (referer_channel != null) {
				if (state != null || nonce != null) queryBuilder.append("&");
				queryBuilder.append("referer_channel=").append(safeUrlEncode(referer_channel));
			}
			
			URI userAgent_goto = URI.create(queryBuilder.toString());
			return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
		}
	}

	private Response sendError(String redirect_uri, String response_mode, String state, String nonce, 
							 String error, String error_description) {
		
		if (redirect_uri == null || redirect_uri.trim().isEmpty()) {
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", error);
			if (error_description != null) {
				errorResponse.put("error_description", error_description);
			}
			return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
		}

		if("form_post".equalsIgnoreCase(response_mode)) {
			Map<String, Object> model = new HashMap<>();
			model.put("error", error);
			if (error_description != null) {
				model.put("error_description", error_description);
			}
			model.put("state", state);
			model.put("nonce", nonce);
			model.put("redirect_uri", redirect_uri);
			return Response.ok(FreeMarkerHelper.createBody("/ImplicitAndHybridFlowResponse.ftl", model)).build();
		} else if ("fragment".equalsIgnoreCase(response_mode)) {
			StringBuilder fragmentBuilder = new StringBuilder(redirect_uri);
			fragmentBuilder.append("#error=").append(safeUrlEncode(error));
			if (error_description != null) {
				fragmentBuilder.append("&error_description=").append(safeUrlEncode(error_description));
			}
			if (state != null) fragmentBuilder.append("&state=").append(safeUrlEncode(state));
			if (nonce != null) fragmentBuilder.append("&nonce=").append(safeUrlEncode(nonce));
			
			URI userAgent_goto = URI.create(fragmentBuilder.toString());
			return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
		} else {
			StringBuilder queryBuilder = new StringBuilder(redirect_uri);
			queryBuilder.append(redirect_uri.contains("?") ? "&" : "?");
			queryBuilder.append("error=").append(safeUrlEncode(error));
			if (error_description != null) {
				queryBuilder.append("&error_description=").append(safeUrlEncode(error_description));
			}
			if (state != null) queryBuilder.append("&state=").append(safeUrlEncode(state));
			if (nonce != null) queryBuilder.append("&nonce=").append(safeUrlEncode(nonce));
			
			URI userAgent_goto = URI.create(queryBuilder.toString());
			return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
		}
	}

	private JsonObject buildTokenAndgetJsonObject(String client_id, String redirect_uri, String state, String nonce, 
												UserToken userToken, List<String> scopes, String code) throws AppException {
		String jwt = tokenService.buildAccessToken(client_id, userToken, scopes, nonce, code);
		JsonReader jsonReader = Json.createReader(new StringReader(jwt));
		JsonObject object = jsonReader.readObject();
		jsonReader.close();
		return object;
	}

	protected String findWhydahUserId(MultivaluedMap<String, String> formParams, HttpServletRequest request) {
		String userTokenId = formParams.getFirst("usertoken_id");
		String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
		if (userTokenIdFromCookie == null) {
			userTokenIdFromCookie = DEVELOPMENT_USER_TOKEN_ID;
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