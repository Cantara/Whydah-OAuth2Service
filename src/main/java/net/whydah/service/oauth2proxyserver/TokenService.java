package net.whydah.service.oauth2proxyserver;

import net.whydah.service.authorizations.UserAuthorization;
import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.service.clients.Client;
import net.whydah.service.clients.ClientService;
import net.whydah.service.errorhandling.AppExceptionCode;
import net.whydah.sso.user.types.UserToken;
import net.whydah.util.AccessTokenMapper;
import net.whydah.util.ClientIDUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by baardl on 09.08.17.
 */
@Service
public class TokenService {
	private static final Logger log = getLogger(TokenService.class);

	private final UserAuthorizationService authorizationService;
	
	private final ClientService clientService;

	@Autowired
	public TokenService(UserAuthorizationService authorizationService, ClientService clientService) {
		this.authorizationService = authorizationService;
		this.clientService = clientService;
	}


	public String buildAccessToken(String client_id, String client_secret, String theUsersAuthorizationCode) throws Exception {
		log.info("buildAccessToken called");
		log.info("buildAccessToken - /token got code: {}", theUsersAuthorizationCode);
		log.info("buildAccessToken - /token got client_id: {}", client_id);
		log.info("buildAccessToken - /token got client_secret: {}", client_secret);

		UserAuthorization userAuthorization = authorizationService.getAuthorization(theUsersAuthorizationCode);
		String accessToken = null;
		if (userAuthorization == null) {
			log.info("The authorization code not found {}", theUsersAuthorizationCode);
			throw AppExceptionCode.AUTHORIZATIONCODE_NOTFOUND_8000;
		} else {

			String userTokenId = userAuthorization.getUserTokenId();
			log.info("Found userTokenId {}", userTokenId);
			UserToken userToken = authorizationService.findUserTokenFromUserTokenId(userTokenId);
			log.info("Found userToken {}", userToken);
			if (userToken != null) {
				log.info("Found userToken {}", userToken);

				Client client = clientService.getClient(userAuthorization.getClientId());
				String applicationId = client.getApplicationId();
				String applicationName = client.getApplicationName();
				String applicationUrl = client.getApplicationUrl();
				List<String> userAuthorizedScopes = userAuthorization.getScopes();
				accessToken = AccessTokenMapper.buildToken(userToken, client_id, applicationId, applicationName, applicationUrl, userAuthorizedScopes);
			} else {
				throw AppExceptionCode.USERTOKEN_INVALID_8001;
			}
		}
		return accessToken;
	}

	private String findApplicationId(String clientId) {

		return ClientIDUtil.getApplicationId(clientId);
	}


	public String buildCode() {
		return UUID.randomUUID().toString();
	}


	public String refreshAccessToken(String client_id, String client_secret, String refresh_token) throws Exception {
		log.info("refreshAccessToken called");
		log.info("refreshAccessToken - /token got refresh_token: {}", refresh_token);
		log.info("refreshAccessToken - /token got client_id: {}", client_id);
		log.info("refreshAccessToken - /token got client_secret: {}", client_secret);

		String[] parts = ClientIDUtil.decrypt(refresh_token).split(":", 2);
		String old_usertoken_id = parts[0];
		log.info("refreshAccessToken - got old_usertoken_id: {}", old_usertoken_id);
		String scopeList = parts[1];

		UserToken userToken = authorizationService.refreshUserTokenFromUserTokenId(old_usertoken_id);
		log.info("refreshAccessToken - got userToken: {}", userToken);
		Client client = clientService.getClient(client_id);
		log.info("refreshAccessToken - got client: {}", client);
		String applicationId = client.getApplicationId();
		String applicationName = client.getApplicationName();
		String applicationUrl = client.getApplicationUrl();
		return AccessTokenMapper.buildToken(userToken, client_id, applicationId, applicationName, applicationUrl, authorizationService.buildScopes(scopeList));
	}
}
