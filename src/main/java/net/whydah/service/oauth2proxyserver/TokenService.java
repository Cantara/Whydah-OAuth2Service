package net.whydah.service.oauth2proxyserver;

import net.whydah.service.authorizations.UserAuthorization;
import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.sso.user.types.UserToken;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by baardl on 09.08.17.
 */
@Service
public class TokenService {
    private static final Logger log = getLogger(TokenService.class);

    private final UserAuthorizationService authorizationService;

    @Autowired
    public TokenService(UserAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }


    public String buildAccessToken(String client_id, String client_secret, String theUsersAuthorizationCode) {
        log.trace("oauth2ProxyServerController - /token got code: {}",theUsersAuthorizationCode);
        log.trace("oauth2ProxyServerController - /token got client_id: {}",client_id);
        log.trace("oauth2ProxyServerController - /token got client_secret: {}",client_secret);

        UserAuthorization userAuthorization = authorizationService.getAuthorization(theUsersAuthorizationCode);
        String accessToken = null;
        if (userAuthorization == null) {
            log.trace("User has not accepted with this code. UserAuthorization were not found in repository for code {}", theUsersAuthorizationCode);
//            accessToken = "{\"access_token\":\"ACCESS_TOKEN\",\"token_type\":\"bearer\",\"expires_in\":2592000,\"refresh_token\":\"REFRESH_TOKEN\",\"scope\":\"read\",\"uid\":22022,\"info\":{\"name\":\"Totto\",\"email\":\"totto@totto.org\"}}";
        } else {
            String scopes = userAuthorization.buildScopeString();
            String userTokenId = userAuthorization.getUserTokenId();
//            String userId = authorizationService.findUserIdFromUserAuthorization(theUsersAuthorizationCode);
            UserToken userToken = authorizationService.findUserTokenFromUserTokenId(userTokenId);
            log.trace("Found userToken {}", userToken);
            accessToken = "{\"access_token\":\"ACCESS_TOKEN\",\"token_type\":\"bearer\",\"expires_in\":2592000,\"refresh_token\":\"REFRESH_TOKEN\",\"scope\":\"" + scopes + "\",\"uid\":22022,\"info\":{\"name\":\"Totto\",\"email\":\"totto@totto.org\"}}";
        }
        return accessToken;
    }



    public String buildCode() {
        return UUID.randomUUID().toString();
    }
}
