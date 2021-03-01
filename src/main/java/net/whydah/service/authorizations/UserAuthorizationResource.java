package net.whydah.service.authorizations;

import net.whydah.service.clients.Client;
import net.whydah.service.clients.ClientService;
import net.whydah.service.errorhandling.AppException;
import net.whydah.service.errorhandling.AppExceptionCode;
import net.whydah.sso.ddd.model.user.UserTokenId;
import net.whydah.sso.user.types.UserToken;
import net.whydah.util.CookieManager;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
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
    		@QueryParam("oauth_session") String oauth_session, 
    		@QueryParam("userticket") String userticket, 
            @Context HttpServletRequest request,
            @Context HttpServletResponse response) throws AppException, UnsupportedEncodingException {
      
    	SSOUserSession session = userAuthorizationService.getSSOSession(oauth_session);
    	if(session==null) {
    		throw AppExceptionCode.SESSION_NOTFOUND_8003;
    	} else {
    		Client client = clientService.getClient(session.getClient_id());
    		if(client==null) {
    			throw AppExceptionCode.CLIENT_NOTFOUND_8002;
    		} else {
    			//solve usertoken
    			UserToken usertoken = null;
    			if(userticket!=null) {
    				usertoken = userAuthorizationService.findUserTokenFromUserTicket(userticket);
    			} else {
    				String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
    				if(userTokenIdFromCookie!=null && UserTokenId.isValid(userTokenIdFromCookie)) {
    					usertoken = userAuthorizationService.findUserTokenFromUserTokenId(userTokenIdFromCookie);
    					if(usertoken==null) {
    						CookieManager.clearUserTokenCookies(request, response);
    					}
    				}
    			}
    			
    			if (usertoken == null) {
					return userAuthorizationService.toSSO(session.getClient_id(), session.getScope(), session.getResponse_type(), session.getState(), session.getNonce(), session.getRedirect_uri());
				} else {
					Map<String, Object> model = userAuthorizationService.buildUserModel(session.getClient_id(), client.getApplicationName(), session.getScope(), session.getResponse_type(), session.getState(), session.getNonce(), session.getRedirect_uri(), usertoken.getUserTokenId());
					Viewable userAuthorizationGui = new Viewable("/UserAuthorization.ftl", model);
					return Response.ok(userAuthorizationGui).build();
				}
    				
    		}
    	}
    }


}
