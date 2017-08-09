package net.whydah.service.oauth2proxyserver;

import net.whydah.service.CredentialStore;
import net.whydah.service.authorizations.UserAuthorization;
import net.whydah.service.authorizations.UserAuthorizationsRepository;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
//            String redirect_uri = uriInfo.getQueryParameters().getFirst("redirect_uri");
//            String client_id = uriInfo.getQueryParameters().getFirst("client_id");
//            String client_secret = uriInfo.getQueryParameters().getFirst("client_secret");
        log.trace("oauth2ProxyServerController - /token got code: {}",theUsersAuthorizationCode);
//            log.trace("oauth2ProxyServerController - /token got redirect_uri: {}",redirect_uri);
        log.trace("oauth2ProxyServerController - /token got client_id: {}",client_id);
        log.trace("oauth2ProxyServerController - /token got client_secret: {}",client_secret);

        UserAuthorization userAuthorization = authorizationsRepository.getAuthorization(theUsersAuthorizationCode);
        String accessToken = null;
        if (userAuthorization == null) {
            accessToken = "{\"access_token\":\"ACCESS_TOKEN\",\"token_type\":\"bearer\",\"expires_in\":2592000,\"refresh_token\":\"REFRESH_TOKEN\",\"scope\":\"read\",\"uid\":22022,\"info\":{\"name\":\"Totto\",\"email\":\"totto@totto.org\"}}";
        } else {
            String scopes = userAuthorization.buildScopeString();
            accessToken = "{\"access_token\":\"ACCESS_TOKEN\",\"token_type\":\"bearer\",\"expires_in\":2592000,\"refresh_token\":\"REFRESH_TOKEN\",\"" + scopes + "\",\"uid\":22022,\"info\":{\"name\":\"Totto\",\"email\":\"totto@totto.org\"}}";
        }
        return accessToken;
    }
}
