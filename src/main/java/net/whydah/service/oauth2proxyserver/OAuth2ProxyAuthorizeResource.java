package net.whydah.service.oauth2proxyserver;

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
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.*;

import static net.whydah.service.authorizations.UserAuthorizationService.DEVELOPMENT_USER_TOKEN_ID;


@Path(OAuth2ProxyAuthorizeResource.OAUTH2AUTHORIZE_PATH)
public class OAuth2ProxyAuthorizeResource {
	public static final String OAUTH2AUTHORIZE_PATH = "/authorize";

	private static final Logger log = LoggerFactory.getLogger(OAuth2ProxyAuthorizeResource.class);
	//private static final Logger log = getLogger(OAuth2ProxyAuthorizeResource.class); -> log is null
	//private static final Logger auditLog = getLogger("auditLog");

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
			@QueryParam("response_mode") String response_mode,
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
			@QueryParam("code_challenge") String code_challenge,
			@DefaultValue("plain") @QueryParam("code_challenge_method") String code_challenge_method,
			//extra custom params
			@DefaultValue("") @QueryParam("logged_in_users") String logged_in_users, //should not show the accept/confirm dialog for existing users
			@DefaultValue("whydah") @QueryParam("referer_channel") String referer_channel, //indicate the referer from which this request is originated
			@Context HttpServletRequest request, @Context HttpServletResponse httpServletResponse) throws MalformedURLException, AppException {
        log.debug("""
                OAuth2ProxyAuthorizeResource - /authorize got response_type: {}
                	scope: {}\s
                	client_id: {}\s
                	redirect_uri: {}\s
                	state: {}\s
                	nonce: {}\
                """, response_type, scope, client_id, redirect_uri, state, nonce);

		if (scope == null) {
			//get default opendid connect scopes
			scope = "openid profile phone email";
		}

		//For purposes of this specification, the default Response Mode for the OAuth 2.0 code Response Type is the query encoding. 
		//For purposes of this specification, the default Response Mode for the OAuth 2.0 token Response Type is the fragment encoding.
		if(response_mode == null) {
			if(response_type.equalsIgnoreCase("code")) {
				response_mode = "query";
			} else {
				response_mode ="fragment";
			}
		}
		
		OAuthenticationSession session = new OAuthenticationSession(scope, response_type, response_mode, client_id, redirect_uri, state, nonce, code_challenge, code_challenge_method, logged_in_users, referer_channel, new Date());
		
		authorizationService.addSSOSession(session);
		
		String directUri = UriComponentsBuilder
				.fromUriString(ConstantValues.MYURI.replaceFirst("/$", "") + "/" + UserAuthorizationResource.USER_PATH)
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
		String response_mode = formParams.getFirst("response_mode");
		String state = formParams.getFirst("state");
		String nonce = formParams.getFirst("nonce");
		String scope = formParams.getFirst("scope");
		String code_challenge = formParams.getFirst("code_challenge");
		String code_challenge_method = formParams.getFirst("code_challenge_method");
		String referer_channel = formParams.getFirst("referer_channel");
		redirect_uri = clientService.getRedirectURI(client_id, redirect_uri).replaceFirst("/$", "");
		
		Client client = clientService.getClient(client_id);
		if (client == null) {
			URI userAgent_goto = URI.create(redirect_uri + "?error=client not found" + "&state=" + state);
			return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
		}
		String userTokenId = formParams.getFirst("usertoken_id");
		UserToken userToken = authorizationService.findUserTokenFromUserTokenId(userTokenId);
		if (userToken == null) {
			return authorizationService.toSSO(client_id, scope, response_type, response_mode, state, nonce, redirect_uri, "", referer_channel, code_challenge, code_challenge_method);
		}

		if ("yes".equals(accepted.trim())) {
			log.info("User accepted authorization. Code {}, FormParams {}", code, formParams);
			return forwardResponse(scope, code, client_id, redirect_uri, response_type, response_mode, state, nonce, userToken, referer_channel, code_challenge, code_challenge_method);
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
		
		String code = tokenService.buildCode();
		
		redirect_uri = clientService.getRedirectURI(client_id, redirect_uri).replaceFirst("/$", "");
		
		Client client = clientService.getClient(client_id);
		if (client == null) {
			URI userAgent_goto = URI.create(redirect_uri + "?error=client not found" + "&state=" + state);
			return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
		}
		
		UserToken userToken = authorizationService.findUserTokenFromUserTokenId(usertoken_id);
		if (userToken == null) {
			return authorizationService.toSSO(client_id, scope, response_type, response_mode, state, nonce, redirect_uri, logged_in_users, referer_channel, code_challenge, code_challenge_method);
		}

		log.info("User implicitly accepted authorization. Code {}, client_id {}, redirect_uri {}, response_type {}, scope {}, state {}, nonce {}, usertoken_id{} ", code, client_id, redirect_uri, response_type, scope, state, nonce, usertoken_id);
		return forwardResponse(scope, code, client_id, redirect_uri, response_type, response_mode, state, nonce, userToken, referer_channel, code_challenge, code_challenge_method);
		
	}
	
	
	public Response forwardResponse(String scope, String code, String client_id,
			String redirect_uri, String response_type, String response_mode, String state, String nonce,
			UserToken userToken, String referer_channel, String code_challenge, String code_challenge_method) {
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
			UserAuthorizationSession userAuthorization = new UserAuthorizationSession(code, scopes, userToken.getUid().toString(), redirect_uri, userToken.getUserTokenId(), nonce, code_challenge, code_challenge_method);
			userAuthorization.setClientId(client_id);
			authorizationService.addAuthorization(userAuthorization);
			URI userAgent_goto = URI.create(redirect_uri 
					+ "?code=" + code 
					+ "&state=" + state 
					+ "&nonce=" + nonce
					+ "&referer_channel=" + referer_channel
					+ ((code_challenge!=null && code_challenge.length()>0)? ("&code_challenge=" + code_challenge + "&code_challenge_method=" + code_challenge_method):"")
					);
			return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
		} else if(response_type.equalsIgnoreCase("token")) {
			try {

				JsonObject object = buildTokenAndgetJsonObject(client_id, redirect_uri, state, nonce, userToken, scopes, null);
				String access_token = object.getString("access_token");
				//HUY added refresh_token back on 14 Nov 2023
				//String refresh_token = object.getString("refresh_token");  //MUST NOT include
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
					URI userAgent_goto = URI.create(redirect_uri + "#access_token=" + access_token +  "&refresh_token=" + refresh_token + "&token_type=" + token_type + "&expires_in=" + expires_in + "&state=" + state + "&nonce=" + nonce + "&referer_channel=" + referer_channel);
					return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
				}
				
			} catch (AppException e) {
				e.printStackTrace();
				return sendError(redirect_uri, response_mode, state, nonce, e.getError());
			} catch (Exception e) {
				e.printStackTrace();
				return sendError(redirect_uri, response_mode, state, nonce, e.getMessage());
			}
			
		} else if(response_type.equalsIgnoreCase("id_token")) {
			try {

				JsonObject object = buildTokenAndgetJsonObject(client_id, redirect_uri, state, nonce, userToken, scopes, null);
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
					URI userAgent_goto = URI.create(redirect_uri + "#id_token=" + id_token +  "&refresh_token=" + refresh_token + "&token_type=" + token_type + "&expires_in=" + expires_in + "&state=" + state + "&nonce=" + nonce + "&referer_channel=" + referer_channel);
					return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
				}
				
				
			} catch (AppException e) {
				e.printStackTrace();
				return sendError(redirect_uri, response_mode, state, nonce, e.getError());
			} catch (Exception e) {
				e.printStackTrace();
				return sendError(redirect_uri, response_mode, state, nonce, e.getMessage());
			}
		} else if(response_type.equalsIgnoreCase("id_token token") || response_type.equalsIgnoreCase("token id_token") ) {
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
					URI userAgent_goto = URI.create(redirect_uri + "#access_token=" + access_token + "&id_token=" + id_token + "&refresh_token=" + refresh_token + "&token_type=" + token_type + "&expires_in=" + expires_in + "&state=" + state + "&nonce=" + nonce + "&referer_channel=" + referer_channel);
					return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
				}
			} catch (AppException e) {
				e.printStackTrace();
				return sendError(redirect_uri, response_mode, state, nonce, e.getError());
			} catch (Exception e) {
				e.printStackTrace();
				return sendError(redirect_uri, response_mode, state, nonce, e.getMessage());
			}
		}  else if(response_type.equalsIgnoreCase("code id_token") || response_type.equalsIgnoreCase("id_token code")) {
			try {
				clientService.addCode(code, nonce);
				//issue a code
				UserAuthorizationSession userAuthorization = new UserAuthorizationSession(code, scopes, userToken.getUid().toString(), redirect_uri, userToken.getUserTokenId(), nonce, code_challenge, code_challenge_method);
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
					 if(code_challenge!=null) {
						 model.put("code_challenge", code_challenge);
						 model.put("code_challenge_method", code_challenge_method);
					 }
					 return Response.ok(FreeMarkerHelper.createBody("/ImplicitAndHybridFlowResponse.ftl", model)).build();
				} else {
					URI userAgent_goto = URI.create(redirect_uri + "#code=" + code 
							+ "&id_token=" + id_token 
							+ "&refresh_token=" + refresh_token
							+ "&token_type=" + token_type 
							+ "&expires_in=" + expires_in 
							+ "&state=" + state 
							+ "&nonce=" + nonce 
							+ "&referer_channel=" + referer_channel
							+ ((code_challenge!=null && code_challenge.length()>0)? ("&code_challenge=" + code_challenge + "&code_challenge_method=" + code_challenge_method):"")
							);
					return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
				}
				
			} catch (AppException e) {
				e.printStackTrace();
				return sendError(redirect_uri, response_mode, state, nonce, e.getError());
			} catch (Exception e) {
				e.printStackTrace();
				return sendError(redirect_uri, response_mode, state, nonce, e.getMessage());
			}
		} else if(response_type.equalsIgnoreCase("code token") || response_type.equalsIgnoreCase("token code")) {
			try {
				clientService.addCode(code, nonce);
				//issue a code
				UserAuthorizationSession userAuthorization = new UserAuthorizationSession(code, scopes, userToken.getUid().toString(), redirect_uri, userToken.getUserTokenId(), nonce, code_challenge, code_challenge_method);
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
					 if(code_challenge!=null) {
						 model.put("code_challenge", code_challenge);
						 model.put("code_challenge_method", code_challenge_method);
					 }
					 
					 return Response.ok(FreeMarkerHelper.createBody("/ImplicitAndHybridFlowResponse.ftl", model)).build();
				} else {
					URI userAgent_goto = URI.create(redirect_uri + "#code=" + code 
							+ "&access_token=" + access_token 
							+ "&refresh_token=" + refresh_token
							+ "&token_type=" + token_type 
							+ "&expires_in=" + expires_in 
							+ "&state=" + state 
							+ "&nonce=" + nonce
							+ "&referer_channel=" + referer_channel
							+ ((code_challenge!=null && code_challenge.length()>0)? ("&code_challenge=" + code_challenge + "&code_challenge_method=" + code_challenge_method):""));
					return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
				}
			} catch (AppException e) {
				e.printStackTrace();
				return sendError(redirect_uri, response_mode, state, nonce, e.getError());
			} catch (Exception e) {
				e.printStackTrace();
				return sendError(redirect_uri, response_mode, state, nonce, e.getMessage());
			}
		} else if (Arrays.asList("code", "id_token", "token").containsAll(Arrays.asList(response_type.split("\\s+")))) {
			try {
				clientService.addCode(code, nonce);
				//issue a code
				UserAuthorizationSession userAuthorization = new UserAuthorizationSession(code, scopes, userToken.getUid().toString(), redirect_uri, userToken.getUserTokenId(), nonce, code_challenge, code_challenge_method);
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
					 if(code_challenge!=null) {
						 model.put("code_challenge", code_challenge);
						 model.put("code_challenge_method", code_challenge_method);
					 }
					 return Response.ok(FreeMarkerHelper.createBody("/ImplicitAndHybridFlowResponse.ftl", model)).build();
				} else {
					URI userAgent_goto = URI.create(redirect_uri + "#code=" + code 
							+ "&id_token=" + id_token 
							+ "&access_token=" + access_token 
							+ "&refresh_token=" + refresh_token
							+ "&token_type=" + token_type 
							+ "&expires_in=" + expires_in 
							+ "&state=" + state 
							+ "&nonce=" + nonce
							+ "&referer_channel=" + referer_channel
							+ ((code_challenge!=null && code_challenge.length()>0)? ("&code_challenge=" + code_challenge + "&code_challenge_method=" + code_challenge_method):"")
							);
					return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
				}
			} catch (AppException e) {
				e.printStackTrace();
				return sendError(redirect_uri, response_mode, state, nonce, e.getError());
			} catch (Exception e) {
				e.printStackTrace();
				return sendError(redirect_uri, response_mode, state, nonce, e.getMessage());
			}
		} else if(response_type.equalsIgnoreCase("none")) {
			if("form_post".equalsIgnoreCase(response_mode)) {
				 Map<String, Object> model = new HashMap<>();
				 model.put("state", state);
				 model.put("nonce", nonce);
				 model.put("redirect_uri", redirect_uri);
				 model.put("referer_channel", referer_channel);
				 Viewable gui = new Viewable("/ImplicitAndHybridFlowResponse.ftl", model);
				 return Response.ok(gui).build();
			} else if("fragment".equalsIgnoreCase(response_mode)) {
				URI userAgent_goto = URI.create(redirect_uri + "#state=" + state + "&nonce=" + nonce+ "&referer_channel=" + referer_channel);
				return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
			} else {
				URI userAgent_goto = URI.create(redirect_uri + "?state=" + state + "&nonce=" + nonce + "&referer_channel=" + referer_channel);
				return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
			}
		} else {
			
			if("form_post".equalsIgnoreCase(response_mode)) {
				 Map<String, Object> model = new HashMap<>();
				 model.put("error", "response_type not supported");
				 model.put("state", state);
				 model.put("nonce", nonce);
				 model.put("redirect_uri", redirect_uri);
				 model.put("referer_channel", referer_channel);
				 return Response.ok(FreeMarkerHelper.createBody("/ImplicitAndHybridFlowResponse.ftl", model)).build();
			} else if("fragment".equalsIgnoreCase(response_mode)) {
				URI userAgent_goto = URI.create(redirect_uri + "#error=response_type not supported" + "&state=" + state + "&nonce=" + nonce + "&referer_channel=" + referer_channel );
				return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
			} else {
				URI userAgent_goto = URI.create(redirect_uri + "?error=response_type not supported" + "&state=" + state + "&nonce=" + nonce + "&referer_channel=" + referer_channel);
				return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
			}
		}
	}


	private Response sendError(String redirect_uri, String response_mode, String state, String nonce, String msg) {
		if("form_post".equalsIgnoreCase(response_mode)) {
//			javax.ws.rs.client.Client client = ClientBuilder.newClient();
//			 WebTarget target = client.target(redirect_uri);
//			 Form form = new Form();
//			 form.param("error", e.getError());
//			 form.param("state", state);
//			 form.param("nonce", nonce);
//			 target.request().post(Entity.entity(form,MediaType.APPLICATION_FORM_URLENCODED_TYPE));
			 Map<String, Object> model = new HashMap<>();
			 model.put("error", msg );
			 model.put("state", state);
			 model.put("nonce", nonce);
			 model.put("redirect_uri", redirect_uri);
			 return Response.ok(FreeMarkerHelper.createBody("/ImplicitAndHybridFlowResponse.ftl", model)).build();
			 
		} else {
			URI userAgent_goto = URI.create(redirect_uri + "#error=" +msg + "&state=" + state + "&nonce=" + nonce );
			return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
		}
	}
	

	private JsonObject buildTokenAndgetJsonObject(String client_id, String redirect_uri, String state, String nonce, UserToken userToken, List<String> scopes, String code) throws AppException {
		String jwt = tokenService.buildAccessToken(client_id, userToken, scopes, nonce, code);
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

