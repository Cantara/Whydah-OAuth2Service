package net.whydah.service.oauth2proxyserver;

import net.whydah.commands.config.ConstantValue;
import net.whydah.service.CredentialStore;
import net.whydah.service.clients.ClientService;
import net.whydah.service.errorhandling.AppException;
import net.whydah.service.errorhandling.AppExceptionCode;
import net.whydah.sso.application.types.Application;
import net.whydah.util.ClientIDUtil;
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
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.List;

@Path(OAuth2ProxyTokenResource.OAUTH2TOKENSERVER_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class OAuth2ProxyTokenResource {
    public static final String OAUTH2TOKENSERVER_PATH = "/token";


    private static final Logger log = LoggerFactory.getLogger(OAuth2ProxyTokenResource.class);
    private static final String ATHORIZATION = "authorization";

    private final CredentialStore credentialStore;
    private final TokenService authorizationService;
    private final ClientService clientService;


    @Autowired
    public OAuth2ProxyTokenResource(CredentialStore credentialStore, TokenService authorizationService, ClientService clientService) {
        this.credentialStore = credentialStore;
        this.authorizationService = authorizationService;
        this.clientService = clientService;
    }


    @GET
    public Response getOauth2ProxyServerController(
    		@QueryParam("grant_type") String grant_type, 
    		@QueryParam("client_id") String client_id, 
    		@QueryParam("client_secret") String client_secret) throws MalformedURLException, AppException {
        log.trace("getOAuth2ProxyServerController - /token got grant_type: {}",grant_type);
        log.trace("getOAuth2ProxyServerController - /token got client_id: {}",client_id);
        log.trace("getOAuth2ProxyServerController - /token got client_secret: {}",client_secret);

        if (credentialStore.hasWhydahConnection()){
            log.trace("getOAuth2ProxyServerController - check STS");
            List<Application> applications = credentialStore.getWas().getApplicationList();
            boolean found_clientId=false;
            for (Application application:applications){
                if (application.getId().equalsIgnoreCase(ClientIDUtil.getApplicationId(client_id))) {
                    log.info("Valid applicationID found ");
                    found_clientId=true;
                    // TODO - Call the STS and return

                }

            }
            if (!found_clientId) {
                log.error("No clientId found");

               throw AppExceptionCode.CLIENT_NOTFOUND_8002;
            }
        }
        log.warn("getOAuth2ProxyServerController - no Whydah - dummy standalone fallback");
        String accessToken = "{ \"access_token\":\"dummy\" }";

        return Response.status(Response.Status.OK).entity(accessToken).build();
    }

    /**
     * Expect Basic Authentication with client_id:client_secret
     * @param grant_type
     * @param code
     * @param scope
     * @param body
     * @param uriInfo
     * @return
     * @throws Exception 
     */
    @POST
    @Consumes("application/x-www-form-urlencoded")
    public Response buildTokenFromFormParameters(
    		@FormParam("grant_type") String grant_type, 
    		@FormParam("code") String code, 
    		@FormParam("refresh_token") String refresh_token,
    		@RequestBody String body,                                     
    		@Context UriInfo uriInfo, 
    		@Context HttpServletRequest request) throws Exception {

        return build(grant_type, code, refresh_token, request);
    }
    
    @POST
    public Response buildToken(
    		@QueryParam("grant_type") String grant_type, 
    		@QueryParam("code") String code,
            @QueryParam("refresh_token") String refresh_token, 
            @RequestBody String body, 
            @Context HttpServletRequest request) throws Exception {

        return build(grant_type, code, refresh_token, request);
    }

    

	private Response build(String grant_type, String code, String refresh_token, HttpServletRequest request) throws Exception {
		Response response = null;
        String basicAuth = request.getHeader(ATHORIZATION);
        String client_id = findClientId(basicAuth);
        String client_secret = findClientSecret(basicAuth);
        if (clientService.isClientValid(client_id)) {
            String accessToken = buildAccessToken(client_id, client_secret, grant_type, code, refresh_token);
            if (accessToken == null) {
                log.error("No accessToken provided");
                response = Response.status(Response.Status.FORBIDDEN).build();
            } else {
                response = Response.ok(accessToken).build();
            }
        } else {
            log.trace("Illegal access from client_id {}", client_id);
            throw AppExceptionCode.CLIENT_NOTFOUND_8002;
        }
        return response;
	}

   

    String findClientId(String basicAuth) {
        String clientId = null;
        String[] credentials = findCredentials(basicAuth);
        if (credentials != null && credentials.length > 0) {
            clientId = credentials[0];
        }
        return clientId;
    }

    String findClientSecret(String basicAuth) {
        String clientSecret = null;
        String[] credentials = findCredentials(basicAuth);
        if (credentials != null && credentials.length > 1) {
            clientSecret = credentials[1];
        }
        return clientSecret;
    }

    String[] findCredentials(String basicAuth ) {
        String[] values = null;
        if (basicAuth != null && basicAuth.startsWith("Basic")) {
            String base64Credentials = basicAuth.substring("Basic".length()).trim();
            String credentials = new String(Base64.getDecoder().decode(base64Credentials),
                    Charset.forName("UTF-8"));
            values = credentials.split(":", 2);
        }
        return values;
    }



    String buildAccessToken(String client_id, String client_secret, String grant_type, String theUsersAuthorizationCode, String refresh_token) throws Exception {

        log.trace("oauth2ProxyServerController - /token got grant_type: {}",grant_type);

        String accessToken = null;
        boolean isClientIdValid = clientService.isClientValid(client_id);
        if (isClientIdValid) {
            accessToken = createAccessToken(client_id, client_secret, grant_type, theUsersAuthorizationCode, refresh_token);
        } else {
        	throw AppExceptionCode.CLIENT_NOTFOUND_8002;
        }
        log.warn("oauth2ProxyServerController - no Whydah - dummy standalone fallback");
        return accessToken;
    }

    protected String createAccessToken(String client_id, String client_secret, String grant_type, String theUsersAuthorizationCode, String refresh_token) throws Exception {
        //TODO find authorization via AuthorizationService, and UserAuthirizationRepository.
        String accessToken = null;
        
        
        
        if ("client_credentials".equalsIgnoreCase(grant_type)){
            accessToken = "{ \"access_token\":\"" + ConstantValue.ATOKEN + "\" }";
        } if ("authorization_code".equalsIgnoreCase(grant_type)){
            accessToken = authorizationService.buildAccessToken(client_id, client_secret, theUsersAuthorizationCode);
        } else if("refresh_token".equalsIgnoreCase(grant_type)) {
        	accessToken = authorizationService.refreshAccessToken(client_id, client_secret, refresh_token);
        }

        return accessToken;
    }

//    private Response processStandaloneResponse(String client_id, String client_secret, String grant_type, String theUsersAuthorizationCode) throws AppException {
//        // Application authentication
//        if ("client_credentials".equalsIgnoreCase(grant_type)){
////            String client_id = uriInfo.getQueryParameters().getFirst("client_id");
////            String client_secret = uriInfo.getQueryParameters().getFirst("client_secret");
//            log.trace("oauth2ProxyServerController - /token got client_id: {}",client_id);
//            log.trace("oauth2ProxyServerController - /token got client_secret: {}",client_secret);
//            // stubbed accesstoken
//            String accessToken = "{ \"access_token\":\"" + ConstantValue.ATOKEN + "\" }";
//            return Response.status(Response.Status.OK).entity(accessToken).build();
//        }
//
//        // User token request
//        if ("authorization_code".equalsIgnoreCase(grant_type)){
//            String accessToken = authorizationService.buildAccessToken(client_id, client_secret, theUsersAuthorizationCode);
//            return Response.status(Response.Status.OK).entity(accessToken).build();
//        }
//        return null;
//    }
//
//    private boolean isValidApplicationID(String appicationID) {
//        List<Application> applications = credentialStore.getWas().getApplicationList();
//
//        for (Application application : applications) {
//            if (application.getId().equalsIgnoreCase(appicationID)) {
//                log.info("Valid applicationID found ");
//                return true;
//            }
//        }
//        return false;
//    }


}

