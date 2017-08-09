package net.whydah.service.oauth2proxyserver;

import net.whydah.commands.config.ConstantValue;
import net.whydah.service.CredentialStore;
import net.whydah.sso.application.types.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.MalformedURLException;
import java.util.List;

import static org.constretto.internal.ConstrettoUtils.isEmpty;

@Path(OAuth2ProxyTokenResource.OAUTH2TOKENSERVER_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class OAuth2ProxyTokenResource {
    public static final String OAUTH2TOKENSERVER_PATH = "/token";


    private static final Logger log = LoggerFactory.getLogger(OAuth2ProxyTokenResource.class);

    private final CredentialStore credentialStore;


    @Autowired
    public OAuth2ProxyTokenResource(CredentialStore credentialStore) {
        this.credentialStore = credentialStore;
    }


    @GET
    public Response getOauth2ProxyServerController(@QueryParam("grant_type") String grant_type, @QueryParam("client_id") String client_id, @QueryParam("client_secret") String client_secret) throws MalformedURLException {
        log.trace("getOAuth2ProxyServerController - /token got grant_type: {}",grant_type);
        log.trace("getOAuth2ProxyServerController - /token got client_id: {}",client_id);
        log.trace("getOAuth2ProxyServerController - /token got client_secret: {}",client_secret);

        if (credentialStore.hasWhydahConnection()){
            log.trace("getOAuth2ProxyServerController - check STS");
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
        log.warn("getOAuth2ProxyServerController - no Whydah - dummy standalone fallback");
        String accessToken = "{ \"access_token\":\"dummy\" }";

        return Response.status(Response.Status.OK).entity(accessToken).build();
    }

    /**
     * Expect Basic Authentication
     * @param client_id
     * @param client_secret
     * @param grant_type
     * @param code
     * @param scope
     * @param body
     * @param uriInfo
     * @return
     * @throws MalformedURLException
     */
    @POST
    @Consumes("application/x-www-form-urlencoded")
    public Response buildTokenFromFormParameters(@FormParam("client_id")String client_id, @FormParam("client_secret")String client_secret,
                                                 @FormParam("grant_type") String grant_type, @FormParam("code") String code, @FormParam("scope") String scope, @RequestBody String body,
                                                 @Context UriInfo uriInfo, @Context HttpServletRequest request) throws MalformedURLException {

//client_id
//        code
//        grant_type
//                scope
        return buildToken(client_id, client_secret, grant_type, code, uriInfo);
    }

    @POST
    public Response buildToken( @QueryParam("client_id") String client_id, @QueryParam("client_secret")String client_secret,
                                        @QueryParam("grant_type") String grant_type, @QueryParam("code") String code, @QueryParam("scope") String scope,@RequestBody String body, @Context UriInfo uriInfo) throws MalformedURLException {

//client_id
//        code
//        grant_type
//                scope
        return buildToken(client_id, client_secret, grant_type, code, uriInfo);
    }

    private Response buildToken(@FormParam("client_id") String client_id, @FormParam("client_secret") String client_secret, @FormParam("grant_type") String grant_type, @FormParam("code") String code, @Context UriInfo uriInfo) {
        if (isEmpty(grant_type)) {
            grant_type = uriInfo.getQueryParameters().getFirst("grant_type");
        }
        log.trace("oauth2ProxyServerController - /token got grant_type: {}",grant_type);

        if (credentialStore.hasWhydahConnection()){
            log.trace("oauth2ProxyServerController - check STS");
            if (client_id.isEmpty()) {
                client_id = uriInfo.getQueryParameters().getFirst("client_id");
            }
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
        Response accessToken = processStandaloneResponse(client_id, client_secret, grant_type, code);
        if (accessToken != null) return accessToken;
        return Response.status(Response.Status.FORBIDDEN).build();
    }


    private Response processStandaloneResponse(String client_id, String client_secret, String grant_type, String code) {
        // Application authentication
        if ("client_credentials".equalsIgnoreCase(grant_type)){
//            String client_id = uriInfo.getQueryParameters().getFirst("client_id");
//            String client_secret = uriInfo.getQueryParameters().getFirst("client_secret");
            log.trace("oauth2ProxyServerController - /token got client_id: {}",client_id);
            log.trace("oauth2ProxyServerController - /token got client_secret: {}",client_secret);
            // stubbed accesstoken
            String accessToken = "{ \"access_token\":\"" + ConstantValue.ATOKEN + "\" }";
            return Response.status(Response.Status.OK).entity(accessToken).build();
        }

        // User token request
        if ("authorization_code".equalsIgnoreCase(grant_type)){
//            String code = uriInfo.getQueryParameters().getFirst("code");
//            String redirect_uri = uriInfo.getQueryParameters().getFirst("redirect_uri");
//            String client_id = uriInfo.getQueryParameters().getFirst("client_id");
//            String client_secret = uriInfo.getQueryParameters().getFirst("client_secret");
            log.trace("oauth2ProxyServerController - /token got code: {}",code);
//            log.trace("oauth2ProxyServerController - /token got redirect_uri: {}",redirect_uri);
            log.trace("oauth2ProxyServerController - /token got client_id: {}",client_id);
            log.trace("oauth2ProxyServerController - /token got client_secret: {}",client_secret);

            String accessToken = "{\"access_token\":\"ACCESS_TOKEN\",\"token_type\":\"bearer\",\"expires_in\":2592000,\"refresh_token\":\"REFRESH_TOKEN\",\"scope\":\"read\",\"uid\":22022,\"info\":{\"name\":\"Totto\",\"email\":\"totto@totto.org\"}}";
            return Response.status(Response.Status.OK).entity(accessToken).build();
        }
        return null;
    }

}

