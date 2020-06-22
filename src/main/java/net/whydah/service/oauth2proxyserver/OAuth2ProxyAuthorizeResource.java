package net.whydah.service.oauth2proxyserver;

import static net.whydah.service.authorizations.UserAuthorizationService.DEVELOPMENT_USER_TOKEN_ID;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;


import net.whydah.commands.config.ConstantValue;
import net.whydah.service.authorizations.UserAuthorization;
import net.whydah.service.authorizations.UserAuthorizationResource;
import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.service.clients.Client;
import net.whydah.service.clients.ClientService;
import net.whydah.service.errorhandling.AppException;
import net.whydah.service.errorhandling.AppExceptionCode;
import net.whydah.sso.user.types.UserToken;
import net.whydah.util.CookieManager;


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
     */
    @GET
    public Response getOauth2ProxyServerController(@QueryParam("response_type") String response_type,
                                                   @QueryParam("scope") String scope,
                                                   @QueryParam("client_id") String client_id,
                                                   @QueryParam("redirect_uri") String redirect_uri,
                                                   @QueryParam("state") String state, 
                                                   @Context HttpServletRequest request, @Context HttpServletResponse httpServletResponse) throws MalformedURLException, AppException {
        log.trace("OAuth2ProxyAuthorizeResource - /authorize got response_type: {}" +
                "\n\tscope: {} \n\tclient_id: {} \n\tredirect_uri: {} \n\tstate: {}", response_type, scope, client_id, redirect_uri, state);
        
        Client client = clientService.getClient(client_id);
        if(client!=null) {

        	//String subPath = "?scope=" + encode(scope) + "&" + "response_type=" + response_type + "&" +"client_id="+ encode(client_id) + "&client_name=" + client.getApplicationName()  + "&" + "redirect_uri=" +redirect_uri + "&" + "state=" + state; 
        	String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
        	if(userTokenIdFromCookie!=null) {
        		//String directUri = "." +UserAuthorizationResource.USER_PATH + subPath;
        		String directUri = UriComponentsBuilder
        				.fromUriString("." +UserAuthorizationResource.USER_PATH)
        				.queryParam("scope", encode(scope))
        				.queryParam("response_type", response_type)
        				.queryParam("client_id", encode(client_id))
        				.queryParam("client_name", encode(client.getApplicationName()))
        				.queryParam("redirect_uri", encode(redirect_uri))
        				.queryParam("state", encode(state))
        				.build().toUriString();
        		
        		
        		URI userAuthorization = URI.create(directUri);
        		return Response.seeOther(userAuthorization).build();
        	} else {

        		String directUri = UriComponentsBuilder
        				.fromUriString(ConstantValue.MYURI + "/" + OAUTH2AUTHORIZE_PATH )
        				.queryParam("scope", encode(scope))
        				.queryParam("response_type", response_type)
        				.queryParam("client_id", encode(client_id))
        				.queryParam("client_name", encode(client.getApplicationName()))
        				.queryParam("redirect_uri", encode(redirect_uri))
        				.queryParam("state", encode(state))
        				.build().toUriString();
        		
        		URI login_redirect = URI.create(ConstantValue.SSO_URI + "/login?redirectURI=" + directUri);
        		 
        		return Response.status(Response.Status.MOVED_PERMANENTLY).location(login_redirect).build();
        		//return Response.seeOther(login_redirect).build();
        	}
        } else {
        	throw AppExceptionCode.CLIENT_NOTFOUND_8002;
        }
    }

    @POST
    @Path("/acceptance")
    @Consumes("application/x-www-form-urlencoded")
    public Response userAcceptance(@FormParam("state") String state, MultivaluedMap<String, String> formParams,@Context HttpServletRequest request) {
        log.trace("Acceptance sent. Values {}", formParams);

        String code = tokenService.buildCode();
        String client_id = formParams.getFirst("client_id");  
        String accepted = formParams.getFirst("accepted");
        
        String redirect_uri = formParams.getFirst("redirect_uri");
        log.info("Resolved redirect_uri from POST form, found:{}", redirect_uri);
        redirect_uri = getRedirectURI(client_id, redirect_uri);
        Client client = clientService.getClient(client_id);
        String userTokenId = formParams.getFirst("usertoken_id");
        UserToken userToken = authorizationService.findUserTokenFromUserTokenId(userTokenId);
        if(userToken==null) {
        	  String subPath = "?scope=" + encode(formParams.getFirst("scope")) + 
    				  "&response_type=" + formParams.getFirst("response_type") + 
    				  "&client_id="+ formParams.getFirst("client_id") + 
    				  "&client_name=" + client.getApplicationName()  + 
    				  "&redirect_uri=" + formParams.getFirst("redirect_uri")  + 
    				  "&state=" + state; 
    		  
    		String url = ConstantValue.MYURI + "/" + OAUTH2AUTHORIZE_PATH + subPath;
        	URI login_redirect = URI.create(ConstantValue.SSO_URI + "/login?redirectURI=" + url);
            return Response.status(Response.Status.MOVED_PERMANENTLY).location(login_redirect).build();
        	//return Response.seeOther(login_redirect).build();
        }
        
        if ("yes".equals(accepted.trim())) {
            auditLog.info("User accepted authorization. Code {}, FormParams {}", code, formParams);
            List<String> scopes = findAcceptedScopes(formParams);
            UserAuthorization userAuthorization = new UserAuthorization(code, scopes, userToken.getUid().toString(), userTokenId);
            userAuthorization.setClientId(client_id);
            authorizationService.addAuthorization(userAuthorization);
            URI userAgent_goto = URI.create(redirect_uri + "?code=" + code + "&state=" + state);
            return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
        } else {
        	//just returns to the current redirect_uri without a code
        	return Response.status(Response.Status.FOUND).location(URI.create(redirect_uri + "?state=" + state)).build();
        }      
    }


	private String getRedirectURI(String client_id, String redirect_url) {
		if (redirect_url == null || redirect_url.isEmpty()) {

            Client client = clientService.getClient(client_id);
            if (client != null) {
                log.info("Resolving redirect_uri from clientService.getClient:{}", client);
                redirect_url = client.getRedirectUrl(); //clientService."http://localhost:8888/oauth/generic/callback";
                log.info("Resolving redirect_uri from clientService.getClient.getRedirectUrl(), found:{}", redirect_url);
            }
        }
        if (redirect_url == null || redirect_url.isEmpty()) {

            Client client = clientService.getClient(client_id);
            if (client != null) {
                redirect_url = client.getApplicationUrl();
                log.info("Resolving redirect_uri from clientService.getClient.getApplicationUrl(), found:{}", redirect_url);
            }
        }
		return redirect_url;
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

    protected List<String> findAcceptedScopes(MultivaluedMap<String, String> formParams) {
        String scope = formParams.getFirst("scope");
        return authorizationService.buildScopes(scope);
    }

    protected String encode(String value) {
        try {
            if (value != null) {
                return URLEncoder.encode(value, "UTF-8");
            } else {
                return "";
            }
        } catch (UnsupportedEncodingException e) {
            log.warn("Encoding exception should not happen. Value {}, Reason {}", value, e.getMessage());
        }
        return value;
    }


}

