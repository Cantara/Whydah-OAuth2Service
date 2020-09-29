package net.whydah.service.authorizations;

import net.whydah.commands.config.ConstantValue;
import net.whydah.service.CredentialStore;
import net.whydah.service.WhydahApplicationSession2;
import net.whydah.sso.commands.adminapi.user.CommandGetUser;
import net.whydah.sso.commands.userauth.CommandGetUsertokenByUserticket;
import net.whydah.sso.commands.userauth.CommandGetUsertokenByUsertokenId;
import net.whydah.sso.commands.userauth.CommandRefreshUserToken;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserToken;
import net.whydah.util.URLHelper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by baardl on 13.08.17.
 */
@Service
public class UserAuthorizationService {
    private static final Logger log = getLogger(UserAuthorizationService.class);
    public static final String DEVELOPMENT_USER_TOKEN_ID = "345460b3-c93e-4150-9808-c62facbadd99";

    private final UserAuthorizationsRepository authorizationsRepository;
    private final SSOUserSessionRepository ssoUserSessionRepository;
    private final CredentialStore credentialStore;

    @Autowired
    public UserAuthorizationService(UserAuthorizationsRepository authorizationsRepository, SSOUserSessionRepository ssoUserSessionRepository, CredentialStore credentialStore) {
        this.authorizationsRepository = authorizationsRepository;
        this.ssoUserSessionRepository = ssoUserSessionRepository;
        this.credentialStore = credentialStore;
    }

    public void addAuthorization(UserAuthorization userAuthorization) {
        authorizationsRepository.addAuthorization(userAuthorization);
    }
    
    public void addSSOSession(SSOUserSession session) {
    	ssoUserSessionRepository.addSession(session);
    }
    
    public SSOUserSession getSSOSession(String sessionId) {
        return ssoUserSessionRepository.getSession(sessionId);
    }
    
    public Response toSSO(String client_id, String scope, String response_type, String state, String redirect_uri) {	
		SSOUserSession session = new SSOUserSession(scope, response_type, client_id, redirect_uri, state);
		addSSOSession(session);
		String directUri = UriComponentsBuilder
				.fromUriString(ConstantValue.MYURI + "/user"  )
				.queryParam("oauth_session", session.getId())
				.build().toUriString();
		
		URI login_redirect = URI.create(ConstantValue.SSO_URI + "/login?redirectURI=" + URLHelper.encode(directUri));
		return Response.status(Response.Status.MOVED_PERMANENTLY).location(login_redirect).build();
	}


    public Map<String, Object> buildUserModel(String clientId, String clientName, String scope, String response_type, String state, String redirect_uri, String userTokenIdFromCookie) {
        final Map<String, String> user = new HashMap<>();
        String name = "Annonymous";
        user.put("id", "-should-not-use-");
        if (userTokenIdFromCookie == null) {
            //FIXME remove stub data
            log.warn("Using stub'ed data for accessing usertokenid");
            userTokenIdFromCookie = DEVELOPMENT_USER_TOKEN_ID;
        }

        UserToken userToken = findUserTokenFromUserTokenId(userTokenIdFromCookie);
        if (userToken != null) {
            name = userToken.getFirstName() + " " + userToken.getLastName();
        }

        user.put("name", name);

        Map<String, Object> model = new HashMap<>();
        model.put("user", user);

        model = addParameter("client_id", clientId, model);
        model = addParameter("client_name", clientName, model);
        model = addParameter("scope", scope, model);
        model = addParameter("response_type", response_type, model);
        model = addParameter("state", state, model);
        model = addParameter("redirect_uri", redirect_uri, model);
        model = addParameter("usertoken_id", userTokenIdFromCookie, model);
        List<String> scopes = buildScopes(scope);
        model.put("scopeList", scopes);
        return model;
    }

    public List<String> buildScopes(String scope) {
        List<String> scopes = new ArrayList<>();
        if (scope != null) {
            String[] scopeArr = scope.split(" ");
            scopes = Arrays.asList(scopeArr);
        }
        return scopes;
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

    public UserAuthorization getAuthorization(String theUsersAuthorizationCode) {
        return authorizationsRepository.getAuthorization(theUsersAuthorizationCode);
    }

    public String findUserIdFromUserAuthorization(String theUsersAuthorizationCode) {
        UserAuthorization userAuthorization = authorizationsRepository.getAuthorization(theUsersAuthorizationCode);
        log.trace("Lookup theUsersAuthorizationCode {}, found authorization {}", theUsersAuthorizationCode, userAuthorization);
        String userId = null;
        if (userAuthorization != null) {
            userId = userAuthorization.getUserId();
        }
        return userId;
    }

    public UserToken findUser(String userId) {
        UserToken userToken = null;
        WhydahApplicationSession2 was = credentialStore.getWas();
        String oauth2AdminTokenId = credentialStore.getAdminUserTokenId();
        String oauth2proxyTokenId = was.getActiveApplicationTokenId();
        String userTokenXml = new CommandGetUser(URI.create(credentialStore.getWas().getUAS()), oauth2proxyTokenId, oauth2AdminTokenId, userId).execute();
        userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);
        return userToken;
    }

    public UserToken findUserTokenFromUserTokenId(String userTokenId) {
        log.info("Attempting to lookup usertokenId:+" + userTokenId);
        String userTokenXml = "";
        try {
            UserToken userToken = null;
            WhydahApplicationSession2 was = credentialStore.getWas();
            URI tokenServiceUri = URI.create(was.getSTS());
            String oauth2proxyTokenId = was.getActiveApplicationTokenId();
            String oauth2proxyAppTokenXml = was.getActiveApplicationTokenXML();
            userTokenXml = new CommandGetUsertokenByUsertokenId(tokenServiceUri, oauth2proxyTokenId, oauth2proxyAppTokenXml, userTokenId).execute();
            log.info("Attempting to lookup userTokenXml:" + userTokenXml);
            userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);
            return userToken;
        } catch (Exception e) {
            log.warn("Unable to parse userTokenXml returned from sts " + userTokenXml + "", e);
            return null;
        }

        //see UserTokenXpathHelper
    }

    public UserToken refreshUserTokenFromUserTokenId(String userTokenId) {
        UserToken userToken = null;
        WhydahApplicationSession2 was = credentialStore.getWas();
        URI tokenServiceUri = URI.create(was.getSTS());
        String oauth2proxyTokenId = was.getActiveApplicationTokenId();
        String oauth2proxyAppTokenXml = was.getActiveApplicationTokenXML();
        String userTokenXml = new CommandRefreshUserToken(tokenServiceUri, oauth2proxyTokenId, oauth2proxyAppTokenXml, userTokenId).execute();
        userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);
        return userToken;

        //see UserTokenXpathHelper
    }

    public UserToken findUserTokenFromUserTicket(String ticket) {
        log.info("Attempting to lookup usertoken by ticket:", ticket);
        String userTokenXml = "";
        try {
            UserToken userToken = null;
            WhydahApplicationSession2 was = credentialStore.getWas();
            URI tokenServiceUri = URI.create(was.getSTS());
            String oauth2proxyTokenId = was.getActiveApplicationTokenId();
            String oauth2proxyAppTokenXml = was.getActiveApplicationTokenXML();
            userTokenXml = new CommandGetUsertokenByUserticket(tokenServiceUri, oauth2proxyTokenId, oauth2proxyAppTokenXml, ticket).execute();
            userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);
            return userToken;
        } catch (Exception e) {
            log.warn("Unable to parse userTokenXml returned from sts " + userTokenXml + "", e);
            return null;
        }

        //see UserTokenXpathHelper
    }
    
}
