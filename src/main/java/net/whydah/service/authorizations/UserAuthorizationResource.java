package net.whydah.service.authorizations;

import net.whydah.commands.config.ConstantValue;
import net.whydah.service.clients.Client;
import net.whydah.service.clients.ClientService;
import net.whydah.service.errorhandling.AppException;
import net.whydah.service.errorhandling.AppExceptionCode;
import net.whydah.util.CookieManager;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Map;

import static net.whydah.service.authorizations.UserAuthorizationResource.USER_PATH;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by baardl on 10.08.17.
 */
@Path(USER_PATH)
public class UserAuthorizationResource {
    private static final Logger log = getLogger(UserAuthorizationResource.class);
    public static final String USER_PATH = "/user";

    private final ClientService clientService;
    private final UserAuthorizationService userAuthorizationService;

    @Autowired
    public UserAuthorizationResource(UserAuthorizationService userAuthorizationService,  ClientService clientService) {
        this.userAuthorizationService = userAuthorizationService;
        this.clientService = clientService;
    }

    /**
     * https://<host>/<context_root>/user?client_id=testclient&scopes=scopes with space delimiter
     * eg: http://localhost:8086/Whydah-OAuth2Service/user?client_id=testclient&scopes=email%20nick
     * @param clientId
     * @param scope
     * @param request
     * @return
     * @throws AppException 
     * @throws UnsupportedEncodingException 
     */
    @GET
    public Response authorizationGui(
    						 @QueryParam("client_id") String clientId, 
    						 @QueryParam("client_name") String clientName, 
    						 @QueryParam("scope") String scope,
    						 @QueryParam("response_type") String responseType,
                             @QueryParam("state") String state,
                             @QueryParam("redirect_uri") String redirect_uri , 
                             @Context HttpServletRequest request) throws AppException, UnsupportedEncodingException {
      
    	Client client = clientService.getClient(clientId);
    	if(client!=null) {
    		
    		String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
    		if(userTokenIdFromCookie ==null) {
    			//String subPath = "?scope=" + encode(scope) + "&" + "response_type=" + responseType + "&" +"client_id="+ encode(clientId) + "&client_name=" + client.getApplicationName()  + "&" + "redirect_uri=" +redirect_uri + "&" + "state=" + state; 
    			String directUri = UriComponentsBuilder
        				.fromUriString(ConstantValue.MYURI + "/" + USER_PATH  )
        				.queryParam("scope", encode(scope))
        				.queryParam("response_type", responseType)
        				.queryParam("client_id", encode(clientId))
        				.queryParam("client_name", encode(client.getApplicationName()))
        				.queryParam("redirect_uri", encode(redirect_uri))
        				.queryParam("state", encode(state))
        				.build().toUriString();
    			
            	URI login_redirect = URI.create(ConstantValue.SSO_URI + "/login?redirectURI=" + directUri);
                return Response.status(Response.Status.MOVED_PERMANENTLY).location(login_redirect).build();
                 
    		} else {
    			Map<String, Object> model = userAuthorizationService.buildUserModel(clientId, clientName, scope, responseType, state, redirect_uri, userTokenIdFromCookie);
    			Viewable userAuthorizationGui =  new Viewable("/UserAuthorization.ftl", model);
    			return Response.ok(userAuthorizationGui).build();
    		}
    	} else {
    		throw AppExceptionCode.CLIENT_NOTFOUND_8002;
    	}
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
