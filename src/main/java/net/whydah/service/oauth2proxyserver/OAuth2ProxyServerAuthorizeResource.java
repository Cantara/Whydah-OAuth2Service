package net.whydah.service.oauth2proxyserver;

import net.whydah.commands.config.ConstantValue;
import net.whydah.service.CredentialStore;
import net.whydah.sso.application.types.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;


@Path(OAuth2ProxyServerAuthorizeResource.OAUTH2AUTHORIZE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class OAuth2ProxyServerAuthorizeResource {
    public static final String OAUTH2AUTHORIZE_PATH = "/authorize";


    private static final Logger log = LoggerFactory.getLogger(OAuth2ProxyServerAuthorizeResource.class);

    private final CredentialStore credentialStore;


    @Autowired
    public OAuth2ProxyServerAuthorizeResource(CredentialStore credentialStore) {
        this.credentialStore = credentialStore;
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
    public Response getOauth2ProxyServerController(@QueryParam("access_type") String access_type, @QueryParam("response_type") String response_type,
                                                   @QueryParam("scope") String scope,@QueryParam("client_id") String client_id,
                                                   @QueryParam("redirect_uri") String redirect_uri, @QueryParam("state") String state) throws MalformedURLException {
        log.trace("OAuth2ProxyServerAuthorizeResource - /authorize got access_type: {},\n\tresponse_type: {}" +
                "\n\tscope: {} \n\tclient_id: {} \n\tredirect_uri: {} \n\tstate: {}",access_type, response_type, scope, client_id, redirect_uri, state);


        String code = "AsT5OjbzRn430zqMLgV3Ia";

        URI userAgent_goto = URI.create("http://localhost:8888/oauth/generic/callback?code=" + code +"&state=" + state);
        return Response.status(Response.Status.FOUND).location(userAgent_goto).build();
    }

    @POST
    public Response oauth2ProxyServerController(@Context UriInfo uriInfo) throws MalformedURLException {


        String grant_type = uriInfo.getQueryParameters().getFirst("grant_type");
        log.trace("oauth2ProxyServerController - /token got grant_type: {}",grant_type);

        if (credentialStore.hasWhydahConnection()){
            log.trace("oauth2ProxyServerController - check STS");
            String client_id = uriInfo.getQueryParameters().getFirst("client_id");
            List<Application> applications = credentialStore.getWas().getApplicationList();
            boolean found_clientId=false;
            for (Application application:applications){
                if (application.getId().equalsIgnoreCase(client_id)){
                    found_clientId=true;
                    // TODO - Call the STS and return

                }

            }
            if (!found_clientId) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
        }


        log.warn("oauth2ProxyServerController - no Whydah - dummy standalone fallback");
        Response accessToken = processStandaloneResponse(uriInfo, grant_type);
        if (accessToken != null) return accessToken;
        return Response.status(Response.Status.FORBIDDEN).build();
    }


    private Response processStandaloneResponse(@Context UriInfo uriInfo, String grant_type) {
        // Application authentication
        if ("client_credentials".equalsIgnoreCase(grant_type)){
            String client_id = uriInfo.getQueryParameters().getFirst("client_id");
            String client_secret = uriInfo.getQueryParameters().getFirst("client_secret");
            log.trace("oauth2ProxyServerController - /token got client_id: {}",client_id);
            log.trace("oauth2ProxyServerController - /token got client_secret: {}",client_secret);
            // stubbed accesstoken
            String accessToken = "{ \"access_token\":\"" + ConstantValue.ATOKEN + "\" }";
            return Response.status(Response.Status.OK).entity(accessToken).build();
        }

        // User token request
        if ("authorization_code".equalsIgnoreCase(grant_type)){
            String code = uriInfo.getQueryParameters().getFirst("code");
            String redirect_uri = uriInfo.getQueryParameters().getFirst("redirect_uri");
            String client_id = uriInfo.getQueryParameters().getFirst("client_id");
            String client_secret = uriInfo.getQueryParameters().getFirst("client_secret");
            log.trace("oauth2ProxyServerController - /token got code: {}",code);
            log.trace("oauth2ProxyServerController - /token got redirect_uri: {}",redirect_uri);
            log.trace("oauth2ProxyServerController - /token got client_id: {}",client_id);
            log.trace("oauth2ProxyServerController - /token got client_secret: {}",client_secret);

            String accessToken = "{\"access_token\":\"ACCESS_TOKEN\",\"token_type\":\"bearer\",\"expires_in\":2592000,\"refresh_token\":\"REFRESH_TOKEN\",\"scope\":\"read\",\"uid\":22022,\"info\":{\"name\":\"Totto\",\"email\":\"totto@totto.org\"}}";
            return Response.status(Response.Status.OK).entity(accessToken).build();
        }
        return null;
    }

}

