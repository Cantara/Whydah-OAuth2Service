package net.whydah.service.oauth2proxyserver;

import net.whydah.service.CredentialStore;
import net.whydah.service.authorizations.UserAuthorization;
import net.whydah.service.authorizations.UserAuthorizationsRepository;
import net.whydah.sso.commands.userauth.CommandGetUsertokenByUsertokenId;
import net.whydah.sso.session.WhydahApplicationSession;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserToken;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by baardl on 09.08.17.
 */
@Service
public class AuthorizationService {
    private static final Logger log = getLogger(AuthorizationService.class);

    private final UserAuthorizationsRepository authorizationsRepository;
    private final CredentialStore credentialStore;

    @Autowired
    public AuthorizationService(UserAuthorizationsRepository authorizationsRepository, CredentialStore credentialStore) {
        this.authorizationsRepository = authorizationsRepository;
        this.credentialStore = credentialStore;
    }


    public String buildAccessToken(String client_id, String client_secret, String theUsersAuthorizationCode) {
        log.trace("oauth2ProxyServerController - /token got code: {}",theUsersAuthorizationCode);
        log.trace("oauth2ProxyServerController - /token got client_id: {}",client_id);
        log.trace("oauth2ProxyServerController - /token got client_secret: {}",client_secret);

        UserAuthorization userAuthorization = authorizationsRepository.getAuthorization(theUsersAuthorizationCode);
        String accessToken = null;
        if (userAuthorization == null) {
            log.trace("User has not accepted with this code. UserAuthorization were not found in repository for code {}", theUsersAuthorizationCode);
            accessToken = "{\"access_token\":\"ACCESS_TOKEN\",\"token_type\":\"bearer\",\"expires_in\":2592000,\"refresh_token\":\"REFRESH_TOKEN\",\"scope\":\"read\",\"uid\":22022,\"info\":{\"name\":\"Totto\",\"email\":\"totto@totto.org\"}}";
        } else {
            String scopes = userAuthorization.buildScopeString();
            String userId = findUserIdFromUserAuthorization(theUsersAuthorizationCode);
            UserToken userToken = findUser(userId);
            accessToken = "{\"access_token\":\"ACCESS_TOKEN\",\"token_type\":\"bearer\",\"expires_in\":2592000,\"refresh_token\":\"REFRESH_TOKEN\",\"" + scopes + "\",\"uid\":22022,\"info\":{\"name\":\"Totto\",\"email\":\"totto@totto.org\"}}";
        }
        return accessToken;
    }

    private String findUserIdFromUserAuthorization(String theUsersAuthorizationCode) {
        UserAuthorization userAuthorization = authorizationsRepository.getAuthorization(theUsersAuthorizationCode);
        log.trace("Lookup theUsersAuthorizationCode {}, found authorization {}", theUsersAuthorizationCode, userAuthorization);
        String userId = null;
        if (userAuthorization != null) {
            userId = userAuthorization.getUserId();
        }
        return userId;
    }

    private UserToken findUser(String userId) {
        //FIXME
        return null;
    }

    public UserToken findUserToken(String userTokenId) {
        UserToken userToken = null;
        WhydahApplicationSession was = credentialStore.getWas();
        URI tokenServiceUri = URI.create(was.getSTS());
        String oauth2proxyTokenId = was.getActiveApplicationTokenId();
        String oauth2proxyAppTokenXml = was.getActiveApplicationTokenXML();
        String userTokenXml = new CommandGetUsertokenByUsertokenId(tokenServiceUri, oauth2proxyTokenId, oauth2proxyAppTokenXml, userTokenId).execute();
        userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);
        return userToken;

                //see UserTokenXpathHelper
    }

    public String buildCode() {
        return "asT5OjbzRn430zqMLgV3Ia";
    }
}
