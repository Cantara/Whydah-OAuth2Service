package net.whydah.service.authorizations;

import net.whydah.util.CookieManager;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
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

    private final UserAuthorizationService userAuthorizationService;

    @Autowired
    public UserAuthorizationResource(UserAuthorizationService userAuthorizationService) {
        this.userAuthorizationService = userAuthorizationService;
    }

    /**
     * https://<host>/<context_root>/user?client_id=testclient&scopes=scopes with space delimiter
     * eg: http://localhost:8086/Whydah-OAuth2Service/user?client_id=testclient&scopes=email%20nick
     * @param clientId
     * @param scope
     * @param request
     * @return
     */
    @GET
    public Viewable authorizationGui(
    						 @QueryParam("client_id") String clientId, 
    						 @QueryParam("scope") String scope,
    						 @QueryParam("response_type") String responseType,
                             @QueryParam("state") String state,
                             @QueryParam("redirect_url") String redirect_url, 
                             @Context HttpServletRequest request) {

        String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
        Map<String, Object> model = userAuthorizationService.buildUserModel(clientId, scope, responseType, state, redirect_url, userTokenIdFromCookie);

        Viewable userAuthorizationGui =  new Viewable("/UserAuthorization.ftl", model);
        return userAuthorizationGui;
    }


}
