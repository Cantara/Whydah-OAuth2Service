package net.whydah.service.authorizations;

import net.whydah.service.oauth2proxyserver.AuthorizationService;
import net.whydah.sso.user.types.UserToken;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by baardl on 13.08.17.
 */
@Service
public class UserAuthorizationService {
    private static final Logger log = getLogger(UserAuthorizationService.class);

    private final AuthorizationService authorizationService;
    private final UserAuthorizationsRepository authorizationsRepository;

    @Autowired
    public UserAuthorizationService(AuthorizationService authorizationService, UserAuthorizationsRepository authorizationsRepository) {
        this.authorizationService = authorizationService;
        this.authorizationsRepository = authorizationsRepository;
    }


    public Map<String, Object> buildUserModel(String clientId, String scope, String code, String state, String redirect_url, String userTokenIdFromCookie) {
        final Map<String,String> user = new HashMap<>();
        String name = "Annonymous";
        user.put("id","-should-not-use-");
        if (userTokenIdFromCookie == null) {
            //FIXME remove stub data
            log.warn("Using stub'ed data for accessing usertokenid");
            userTokenIdFromCookie = "4efd7770-9b03-48c8-8992-5e9a5d06e45e";
        }

        UserToken userToken = authorizationService.findUserToken(userTokenIdFromCookie);
        if (userToken != null) {
            name = userToken.getFirstName() + " " + userToken.getLastName();
        }

        user.put("name", name);

        Map<String, Object> model = new HashMap<>();
        model.put("user", user);
        model = addParameter("client_id", clientId, model);
        model = addParameter("scope", scope, model);
        model = addParameter("code", code, model);
        model = addParameter("state", state, model);
        model = addParameter("redirect_url", redirect_url, model);
        model = addParameter("usertoken_id", userTokenIdFromCookie, model);
        List<String> scopes = new ArrayList<>();
        if (scope != null) {
            String[] scopeArr = scope.split(" ");
            scopes = Arrays.asList(scopeArr);
        }
        model.put("scopeList", scopes);
        return model;
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
