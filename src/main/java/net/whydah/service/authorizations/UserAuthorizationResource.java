package net.whydah.service.authorizations;

import net.whydah.util.CookieManager;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import java.util.*;

import static net.whydah.service.authorizations.UserAuthorizationResource.USER_PATH;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by baardl on 10.08.17.
 */
@Path(USER_PATH)
public class UserAuthorizationResource {
    private static final Logger log = getLogger(UserAuthorizationResource.class);
    public static final String USER_PATH = "/user";
    //import org.glassfish.jersey.server.mvc.Viewable;

    /**
     * https://<host>/<context_root>/user?client_id=testclient&scopes=scopes with space delimiter
     * eg: http://localhost:8086/Whydah-OAuth2Service/user?client_id=testclient&scopes=email%20nick
     * @param clientId
     * @param scope
     * @param request
     * @return
     */
    @GET
    public Viewable authorizationGui(@QueryParam("client_id") String clientId, @QueryParam("scope") String scope,
                             @QueryParam("code") String code,
                             @QueryParam("state") String state,
                             @QueryParam("redirect_url") String redirect_url,@Context HttpServletRequest request) {
        String userTokenIdFromCookie = CookieManager.getUserTokenIdFromCookie(request);
        if (userTokenIdFromCookie == null) {
            userTokenIdFromCookie = "";
        }
        final Map<String,String> user = new HashMap<>();
        user.put("name", userTokenIdFromCookie);
        user.put("id","1224");

        Map<String, Object> model = new HashMap<>();
        model.put("user", user);
        model = addParameter("client_id", clientId, model);
        model = addParameter("scope", scope, model);
        model = addParameter("code", code, model);
        model = addParameter("state", state, model);
        model = addParameter("redirect_url", redirect_url, model);

        List<String> scopes = new ArrayList<>();
        if (scope != null) {
            String[] scopeArr = scope.split(" ");
            scopes = Arrays.asList(scopeArr);
        }

        model.put("scopeList", scopes);

        Viewable userAuthorizationGui =  new Viewable("/UserAuthorization.ftl", model);
        return userAuthorizationGui;
    }



    protected Map<String, Object> addParameter(String key, String value, Map<String, Object> map) {
        if (key != null && map != null) {
          if (value == null) {
              map.put(key, "");
          } else {
              map.put(key, value);
          }
        }
        return map;

    }
}
