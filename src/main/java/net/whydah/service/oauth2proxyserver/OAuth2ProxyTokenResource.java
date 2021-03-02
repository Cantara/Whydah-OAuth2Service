package net.whydah.service.oauth2proxyserver;

import net.whydah.service.CredentialStore;
import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.service.clients.ClientService;
import net.whydah.service.errorhandling.AppException;
import net.whydah.service.errorhandling.AppExceptionCode;
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
import java.nio.charset.Charset;
import java.util.Base64;

@Path(OAuth2ProxyTokenResource.OAUTH2TOKENSERVER_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class OAuth2ProxyTokenResource {
    public static final String OAUTH2TOKENSERVER_PATH = "/token";


    private static final Logger log = LoggerFactory.getLogger(OAuth2ProxyTokenResource.class);

    private static final String ATHORIZATION = "authorization";

    private final CredentialStore credentialStore;
    private final UserAuthorizationService authService;
    private final TokenService tokenService;
    private final ClientService clientService;


    @Autowired
    public OAuth2ProxyTokenResource(CredentialStore credentialStore, TokenService authorizationService, ClientService clientService, UserAuthorizationService authService) {
        this.credentialStore = credentialStore;
        this.tokenService = authorizationService;
        this.clientService = clientService;
        this.authService = authService;
    }


    @GET
    public Response buildTokenGet(
            @QueryParam("grant_type") String grant_type, //must set to grant_type=authorization_code or grant_type=refresh_token or grant_type=client_credentials or grant_type=password
            @QueryParam("code") String code,//required if grant_type=authorization_code
            @QueryParam("nonce") String nonce, //required if this was included in the authorization request
            @QueryParam("redirect_uri") String redirect_uri, //required if this was included in the authorization request
            @QueryParam("refresh_token") String refresh_token, //required if grant_type=refresh_token
            @QueryParam("username") String username, //required if this was grant_type=password
            @QueryParam("password") String password, //required if this was grant_type=password
            @QueryParam("client_id") String client_id, //required if not specified in the authorization header
            @QueryParam("client_secret") String client_secret, //required if not specified in the authorization header
            @RequestBody String body,
            @Context HttpServletRequest request) throws Exception, AppException {


        String basicAuth = request.getHeader(ATHORIZATION);
        if (basicAuth != null) {
            client_id = findClientId(basicAuth);
            log.info("buildTokenGet query clientId:" + client_id);
            client_secret = findClientSecret(basicAuth);
            log.info("buildTokenGet query client_secret:" + client_secret);
        }
        log.info("buildTokenGet query param nonce:" + nonce);

        return build(client_id, client_secret, grant_type, code, nonce, redirect_uri, refresh_token, username, password);
    }


    /**
     * https://tools.ietf.org/html/rfc6749#section-4.1.3
     *  grant_type
     REQUIRED.  Value MUST be set to "authorization_code".

     code
     REQUIRED.  The authorization code received from the
     authorization server.
	
	   redirect_uri
	         REQUIRED, if the "redirect_uri" parameter was included in the
	         authorization request as described in Section 4.1.1, and their
	         values MUST be identical.

     client_id
     REQUIRED, if the client is not authenticating with the
     authorization server as described in Section 3.2.1.
     *
     *
     * Expect Basic Authentication with client_id:client_secret
     *
     * @param grant_type
     * @param code
     * @param nonce
     * @param scope
     * @param body
     * @param uriInfo
     * @return
     * @throws Exception
     * @throws AppException
     */
    @POST
    @Consumes("application/x-www-form-urlencoded")
    public Response buildTokenFromFormParameters(
            @QueryParam("grant_type") String q_grant_type, //must set to grant_type=authorization_code or grant_type=refresh_token or grant_type=client_credentials or grant_type=password
            @QueryParam("code") String q_code,//required if grant_type=authorization_code
            @QueryParam("nonce") String q_nonce, //required if this was included in the authorization request
            @QueryParam("redirect_uri") String q_redirect_uri, //required if this was included in the authorization request
            @QueryParam("refresh_token") String q_refresh_token, //required if grant_type=refresh_token
            @QueryParam("username") String q_username, //required if this was grant_type=password
            @QueryParam("password") String q_password, //required if this was grant_type=password
            @QueryParam("client_id") String q_client_id, //required if not specified in the authorization header
            @QueryParam("client_secret") String q_client_secret, //required if not specified in the authorization header

            @FormParam("grant_type") String grant_type, //must set to grant_type=authorization_code or grant_type=refresh_token or grant_type=client_credentials or grant_type=password
            @FormParam("code") String code, //required if grant_type=authorization_code
            @FormParam("nonce") String nonce, //required if this was included in the authorization request
            @FormParam("redirect_uri") String redirect_uri, //required if this was included in the authorization request
            @FormParam("refresh_token") String refresh_token, //required if grant_type=refresh_token
            @FormParam("username") String username, //required if this was grant_type=password
            @FormParam("password") String password, //required if this was grant_type=password
            @RequestBody String body,
            @Context UriInfo uriInfo,
            @Context HttpServletRequest request) throws Exception, AppException {

    	String client_id = null;
        String client_secret = null;
        String basicAuth = request.getHeader(ATHORIZATION);
        if (basicAuth != null) {
            client_id = findClientId(basicAuth);
            log.info("buildTokenFromFormParameters form clientId:" + client_id);
            client_secret = findClientSecret(basicAuth);
            log.info("buildTokenFromFormParameters form client_secret:" + client_secret);
        } else {
            throw AppExceptionCode.MISC_MISSING_PARAMS_9998.setErrorDescription("Missing client_id parameter");
        }
        if (nonce == null) {
            nonce = q_nonce;
        }
        if (grant_type == null) {
            grant_type = q_grant_type;
        }
        if (client_id == null) {
            client_id = q_client_id;
        }
        if (redirect_uri == null) {
            redirect_uri = q_redirect_uri;
        }

        log.info("buildTokenFromFormParameters form param nonce:" + nonce);
        return build(client_id, client_secret, grant_type, code, nonce, redirect_uri, refresh_token, username, password);
    }

    @POST
    public Response buildTokenPost(
            @QueryParam("grant_type") String grant_type, //must set to grant_type=authorization_code or grant_type=refresh_token or grant_type=client_credentials or grant_type=password
            @QueryParam("code") String code,//required if grant_type=authorization_code
            @QueryParam("nonce") String nonce, //required if this was included in the authorization request
            @QueryParam("redirect_uri") String redirect_uri, //required if this was included in the authorization request
            @QueryParam("refresh_token") String refresh_token, //required if grant_type=refresh_token
            @QueryParam("username") String username, //required if this was grant_type=password
            @QueryParam("password") String password, //required if this was grant_type=password
            @QueryParam("client_id") String client_id, //required if not specified in the authorization header
            @QueryParam("client_secret") String client_secret, //required if not specified in the authorization header
            @RequestBody String body,
            @Context HttpServletRequest request) throws Exception, AppException {


        String basicAuth = request.getHeader(ATHORIZATION);
        if (basicAuth != null) {
            client_id = findClientId(basicAuth);
            log.info("buildTokenPost query clientId:" + client_id);
            client_secret = findClientSecret(basicAuth);
            log.info("buildTokenPost query client_secret:" + client_secret);
        }
        log.info("buildTokenPost query param nonce:" + nonce);

        return build(client_id, client_secret, grant_type, code, nonce, redirect_uri, refresh_token, username, password);
    }


    private Response build(String client_id, String client_secret, String grant_type, String code, String nonce, String redirect_uri, String refresh_token, String username, String password) throws Exception, AppException {
        Response response = null;
        log.info("build nonce:" + nonce);
        if (clientService.isClientValid(client_id)) {
            String accessToken = tokenService.buildAccessToken(client_id, client_secret, grant_type, code, nonce, redirect_uri, refresh_token, username, password);
            if (accessToken == null) {
                if ("refresh_token".equalsIgnoreCase(grant_type)) {
                    log.warn("Unable to renew user session");
                    response = Response.status(Response.Status.GONE).build();
                } else {
                    log.error("No accessToken provided");
                    response = Response.status(Response.Status.FORBIDDEN).build();
                }
            } else {
                log.info("accessToken provided:" + accessToken);
                response = Response.ok(accessToken).build();
            }
        } else {
            log.warn("Illegal access from client_id {}", client_id);
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
        log.info("Found clientId:" + clientId + " from basicAuth:" + basicAuth);
        return clientId;
    }

    String findClientSecret(String basicAuth) {
        String clientSecret = null;
        String[] credentials = findCredentials(basicAuth);
        if (credentials != null && credentials.length > 1) {
            clientSecret = credentials[1];
        }
        log.info("Found clientSecret:" + clientSecret + " from basicAuth:" + basicAuth);
        return clientSecret;
    }

    String[] findCredentials(String basicAuth) {
        String[] values = null;
        if (basicAuth != null && basicAuth.startsWith("Basic")) {
            String base64Credentials = basicAuth.substring("Basic".length()).trim();
            String credentials = new String(Base64.getDecoder().decode(base64Credentials),
                    Charset.forName("UTF-8"));
            values = credentials.split(":", 2);
        }
        return values;
    }


  

}

