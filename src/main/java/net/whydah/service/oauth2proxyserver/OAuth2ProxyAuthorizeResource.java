package net.whydah.service.oauth2proxyserver;

import net.whydah.service.authorizations.UserAuthorizationResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLEncoder;


@Path(OAuth2ProxyAuthorizeResource.OAUTH2AUTHORIZE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class OAuth2ProxyAuthorizeResource {
    public static final String OAUTH2AUTHORIZE_PATH = "/authorize";


    private static final Logger log = LoggerFactory.getLogger(OAuth2ProxyAuthorizeResource.class);

    private final AuthorizationService authorizationService;


    @Autowired
    public OAuth2ProxyAuthorizeResource( AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
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
    public Response userAcceptance(@FormParam("state") String state, MultivaluedMap<String, String> formParams) {
        log.trace("Acceptance sent. Values {}", formParams);

        String code = authorizationService.buildCode();

        //TODO add UserAuthorization with code and user info.
        URI userAgent_goto = URI.create("http://localhost:8888/oauth/generic/callback?code=" + code +"&state=" + state);
        return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
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

