package net.whydah.service.authorizations;

import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    //import org.glassfish.jersey.server.mvc.Viewable;

    @GET
//    @Produces(MediaType.TEXT_HTML)
    public Viewable getHello() {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("user", "Pavel");
        final List<String> list = new ArrayList<String>();
        list.add("item1");
        list.add("item2");
        list.add("item3");
        map.put("items", list);

        Viewable userA =  new Viewable("/UserAuthorization.ftl", map);
        return userA;
    }
}
