package net.whydah.service.oauth2proxyserver;

import net.whydah.service.authorizations.UserAuthorization;
import net.whydah.service.authorizations.UserAuthorizationResource;
import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.service.clients.Client;
import net.whydah.service.clients.ClientService;
import net.whydah.sso.user.types.UserToken;
import net.whydah.util.CookieManager;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;

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
     */
    @GET
    public Response getOauth2ProxyServerController(@QueryParam("access_type") String access_type,
                                                   @QueryParam("response_type") String response_type,
                                                   @QueryParam("scope") String scope,
                                                   @QueryParam("client_id") String client_id,
                                                   @QueryParam("redirect_uri") String redirect_uri,
                                                   @QueryParam("state") String state) throws MalformedURLException {
        log.trace("OAuth2ProxyAuthorizeResource - /authorize got access_type: {},\n\tresponse_type: {}" +
                "\n\tscope: {} \n\tclient_id: {} \n\tredirect_uri: {} \n\tstate: {}",access_type, response_type, scope, client_id, redirect_uri, state);

        String url = "." +UserAuthorizationResource.USER_PATH + "?scope=" + encode(scope) + "&" + "response_type=" + response_type + "&" +
                "client_id="+ client_id + "&" + "redirect_uri=" +redirect_uri + "&" + "state=" + state;
        URI userAuthorization = URI.create(url);
        return Response.seeOther(userAuthorization).build();


    }

    @POST
    @Path("/acceptance")
    @Consumes("application/x-www-form-urlencoded")
    public Response userAcceptance(@FormParam("state") String state, MultivaluedMap<String, String> formParams,@Context HttpServletRequest request) {
        log.trace("Acceptance sent. Values {}", formParams);

        String code = tokenService.buildCode();

        String accepted = formParams.getFirst("accepted");
        if ("yes".equals(accepted.trim())) {
            auditLog.info("User accepted authorization. Code {}, FormParams {}", code, formParams);
            List<String> scopes = findAcceptedScopes(formParams);
            String whydahUserId = findWhydahUserId(formParams, request);
            if (whydahUserId != null) {
                UserAuthorization userAuthorization = new UserAuthorization(code, scopes, whydahUserId);
                authorizationService.addAuthorization(userAuthorization);
            }
        }

        //TODO add UserAuthorization with code and user info.
        String redirect_url = formParams.getFirst("redirect_url");
        if (redirect_url == null || redirect_url.isEmpty()) {
            String client_id = formParams.getFirst("client_id");
            Client client = clientService.getClient(client_id);
            if (client != null) {
                redirect_url = client.getRedirectUrl(); //clientService."http://localhost:8888/oauth/generic/callback";
            }
        }
        URI userAgent_goto = URI.create(redirect_url + "?code=" + code +"&state=" + state);
        return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
    }

    private String findWhydahUserId(MultivaluedMap<String, String> formParams, HttpServletRequest request) {
        String userTokenId = formParams.getFirst("usertoken_id");
        String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
        //Validate that usertoken has stayed the same. Ie user has not loged into another account.
        if (userTokenIdFromCookie == null) {
            userTokenIdFromCookie = "4efd7770-9b03-48c8-8992-5e9a5d06e45e"; //FIXME temporary
        }
        String whydahUserId = null;
        if (userTokenId != null && userTokenId.equals(userTokenIdFromCookie)) {
            UserToken userToken = authorizationService.findUserTokenFromUserTokenId(userTokenId);
            if (userToken != null) {
                whydahUserId = userToken.getUid();
            }
        }
        if (whydahUserId == null) {
            whydahUserId = "useradmin";  //FIXME temporary
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

