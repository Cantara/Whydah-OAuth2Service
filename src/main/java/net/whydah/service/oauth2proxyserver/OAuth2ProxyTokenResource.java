package net.whydah.service.oauth2proxyserver;

import java.nio.charset.Charset;
import java.util.Base64;

import org.glassfish.hk2.api.Immediate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import net.whydah.service.CredentialStore;
import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.service.clients.ClientService;
import net.whydah.service.errorhandling.AppException;
import net.whydah.service.errorhandling.AppExceptionCode;

@Path(OAuth2ProxyTokenResource.OAUTH2TOKENSERVER_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Component
@Immediate
public class OAuth2ProxyTokenResource {
    public static final String OAUTH2TOKENSERVER_PATH = "/token";


    private static final Logger log = LoggerFactory.getLogger(OAuth2ProxyTokenResource.class);

    private static final String ATHORIZATION = "authorization";

    @Inject
    private CredentialStore credentialStore;

    @Inject
    private UserAuthorizationService authService;

    @Inject
    private TokenService tokenService;

    @Inject
    private ClientService clientService;

    // Add this default constructor
    public OAuth2ProxyTokenResource() {
        // Empty constructor for Jersey
    }


    @Inject
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
            @QueryParam("code_verifier") String code_verifier,
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

        return build(client_id, client_secret, grant_type, code, nonce, redirect_uri, refresh_token, username, password, code_verifier);
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
            @QueryParam("code_verifier") String q_code_verifier,
            
            @FormParam("grant_type") String grant_type, //must set to grant_type=authorization_code or grant_type=refresh_token or grant_type=client_credentials or grant_type=password
            @FormParam("code") String code, //required if grant_type=authorization_code
            @FormParam("nonce") String nonce, //required if this was included in the authorization request
            @FormParam("redirect_uri") String redirect_uri, //required if this was included in the authorization request
            @FormParam("refresh_token") String refresh_token, //required if grant_type=refresh_token
            @FormParam("username") String username, //required if this was grant_type=password
            @FormParam("password") String password, //required if this was grant_type=password
            @FormParam("code_verifier") String code_verifier,
            @FormParam("client_id") String client_id,
            @RequestBody String body,
            @Context UriInfo uriInfo,
            @Context HttpServletRequest request) throws Exception, AppException {
        try {
            
            String client_secret = null;
            String basicAuth = request.getHeader(ATHORIZATION);
            if (basicAuth != null) {
                client_id = findClientId(basicAuth);
                log.info("buildTokenFromFormParameters form clientId:" + client_id);
                client_secret = findClientSecret(basicAuth);
                log.info("buildTokenFromFormParameters form client_secret:" + client_secret);
            }

            if (code == null) {
                code = q_code;
            }
            if (nonce == null) {
                nonce = q_nonce;
            }
            if (nonce == null && code != null) {
                nonce = clientService.getNonce(code);

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
            if (refresh_token == null) {
                refresh_token = q_refresh_token;
            }
            if (username == null) {
                username = q_username;
            }
            if (password == null) {
                password = q_password;
            }

            if (client_id == null) {
                throw AppExceptionCode.MISC_MISSING_PARAMS_9998;
            }


            log.info("buildTokenFromFormParameters form param nonce:" + nonce);
            return build(client_id, client_secret, grant_type, code, nonce, redirect_uri, refresh_token, username, password, code_verifier);
        } catch (Throwable t) {
            log.error("", t);
            throw t;
        }
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
            @QueryParam("code_verifier") String code_verifier,
            
            @RequestBody String body,
            @Context HttpServletRequest request) throws Exception, AppException {

        try {
            String basicAuth = request.getHeader(ATHORIZATION);
            if (basicAuth != null) {
                client_id = findClientId(basicAuth);
                log.info("buildTokenPost query clientId:" + client_id);
                client_secret = findClientSecret(basicAuth);
                log.info("buildTokenPost query client_secret:" + client_secret);
            }
            log.info("buildTokenPost query param nonce:" + nonce);

            return build(client_id, client_secret, grant_type, code, nonce, redirect_uri, refresh_token, username, password, code_verifier);
        } catch (Throwable t) {
            log.error("", t);
            throw t;
        }
    }


    private Response build(String client_id, String client_secret, String grant_type, String code, String nonce, String redirect_uri, String refresh_token, String username, String password, String code_verifier) throws Exception, AppException {
        Response response = null;
        log.info("build nonce:" + nonce);
        if (clientService.isClientValid(client_id)) {
            String token = tokenService.buildToken(client_id, client_secret, grant_type, code, nonce, redirect_uri, refresh_token, username, password, code_verifier);
            if (token == null) {
                if ("refresh_token".equalsIgnoreCase(grant_type)) {
                    log.warn("Unable to renew user session");
                    response = Response.status(Response.Status.GONE).build();
                } else {
                    log.error("No token provided");
                    response = Response.status(Response.Status.FORBIDDEN).build();
                }
            } else {
                log.info("Token provided:" + token);
                response = Response.ok(token)
                		.header("Cache-Control", "no-store")
                		.header("Pragma", "no-cache")
                		.build();
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

