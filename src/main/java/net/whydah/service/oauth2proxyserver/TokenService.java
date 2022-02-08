package net.whydah.service.oauth2proxyserver;

import net.whydah.commands.config.ConstantValues;
import net.whydah.service.authorizations.UserAuthorization;
import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.service.clients.Client;
import net.whydah.service.clients.ClientService;
import net.whydah.service.errorhandling.AppException;
import net.whydah.service.errorhandling.AppExceptionCode;
import net.whydah.sso.application.helpers.ApplicationXpathHelper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.commands.appauth.CommandLogonApplication;
import net.whydah.sso.commands.userauth.CommandLogonUserByUserCredential;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserCredential;
import net.whydah.sso.user.types.UserToken;
import net.whydah.util.AccessTokenMapper;
import net.whydah.util.ClientIDUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
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


	public String buildAccessToken(String client_id, String client_secret, String grant_type, String code, String nonce, String redirect_uri, String refresh_token, String username, String password) throws Exception, AppException {

		log.info("TokenService - /token got grant_type: {}", grant_type);
		log.info("buildAccessToken - /token got client_id: {}", client_id);
		log.info("buildAccessToken - /token got nonce: {}", nonce);

		String accessToken = null;
		boolean isClientIdValid = clientService.isClientValid(client_id);
		if (isClientIdValid) {
			log.info("TokenService - isClientIdValid: {}", isClientIdValid);
			accessToken = createAccessToken(client_id, grant_type, code, refresh_token, username, password, nonce);
		} else {
			log.info("TokenService - isClientIdValid: {}", isClientIdValid);
			throw AppExceptionCode.CLIENT_NOTFOUND_8002;
		}
		log.warn("TokenService - no Whydah - dummy standalone fallback");
		return accessToken;
	}

	protected String createAccessToken(String client_id, String grant_type, String code, String refresh_token, String username, String password, String nonce) throws Exception, AppException {

		log.info("TokenService - createAccessToken -grant type:" + grant_type);
		log.info("createAccessToken - /token got nonce: {}", nonce);

		String accessToken = null;
		if ("client_credentials".equalsIgnoreCase(grant_type)) {
			log.info("TokenService - createAccessToken - client_credentials");
			accessToken = buildAccessToken(client_id, nonce);
		} else if ("password".equalsIgnoreCase(grant_type)) {
			log.info("TokenService - createAccessToken - password");
			//log on to the app with the user credentials 
			Application app = clientService.getApplicationByClientId(client_id);
			String myAppTokenXml = new CommandLogonApplication(URI.create(ConstantValues.STS_URI), new ApplicationCredential(app.getId(), app.getName(), app.getSecurity().getSecret())).execute();
			String myApplicationTokenID = ApplicationXpathHelper.getAppTokenIdFromAppTokenXml(myAppTokenXml);
			String userticket = UUID.randomUUID().toString();
			String userToken = new CommandLogonUserByUserCredential(URI.create(ConstantValues.STS_URI), myApplicationTokenID, myAppTokenXml, new UserCredential(username, password), userticket).execute();
			UserToken ut = UserTokenMapper.fromUserTokenXml(userToken);
			//build token
			accessToken = buildAccessToken(client_id, ut, authorizationService.buildScopes("openid profile phone email"), nonce);
		}
		if ("authorization_code".equalsIgnoreCase(grant_type)) {
			log.info("TokenService - createAccessToken - authorization_code");
			accessToken = buildAccessToken(client_id, code, nonce);
		} else if ("refresh_token".equalsIgnoreCase(grant_type)) {
			log.info("TokenService - createAccessToken - refresh_token");
			accessToken = refreshAccessToken(client_id, refresh_token, nonce);
		}
		log.info("TokenService - createAccessToken - accessToken:" + accessToken);
		return accessToken;
	}

	public String buildAccessToken(String client_id, String usertokenId, List<String> userAuthorizedScopes, String nonce) throws AppException {
		log.info("buildAccessToken called");
		log.info("buildAccessToken - /token got client_id: {}", client_id);
		log.info("buildAccessToken - /token got nonce: {}", nonce);

		String accessToken = null;

		log.info("Found userTokenId {}", usertokenId);
		UserToken userToken = authorizationService.findUserTokenFromUserTokenId(usertokenId);
		log.info("Found userToken {}", userToken);
		if (userToken != null) {
			log.info("Found userToken {}", userToken);
			Client client = clientService.getClient(client_id);
			String applicationId = client.getApplicationId();
			String applicationName = client.getApplicationName();
			String applicationUrl = client.getApplicationUrl();
			Map<String, Set<String>> jwtRolesByScope = client.getJwtRolesByScope();
			accessToken = AccessTokenMapper.buildToken(userToken, client_id, applicationId, applicationName, applicationUrl, nonce, userAuthorizedScopes, jwtRolesByScope);
		} else {
			throw AppExceptionCode.USERTOKEN_INVALID_8001;
		}


		return accessToken;
	}

	public String buildAccessToken(String client_id, UserToken usertoken, List<String> userAuthorizedScopes, String nonce) throws AppException {
		log.info("buildAccessToken called");
		log.info("buildAccessToken - /token got client_id: {}", client_id);
		log.info("buildAccessToken - /token got nonce: {}", nonce);
		String accessToken = null;
		if (nonce == null) {
			nonce = "";
		}
		if (usertoken != null) {
			log.info("Found userToken {}", usertoken);
			Client client = clientService.getClient(client_id);
			String applicationId = client.getApplicationId();
			String applicationName = client.getApplicationName();
			String applicationUrl = client.getApplicationUrl();
			Map<String, Set<String>> jwtRolesByScope = client.getJwtRolesByScope();
			accessToken = AccessTokenMapper.buildToken(usertoken, client_id, applicationId, applicationName, applicationUrl, nonce, userAuthorizedScopes, jwtRolesByScope);
		} else {
			throw AppExceptionCode.USERTOKEN_INVALID_8001;
		}
		return accessToken;
	}

	public String buildAccessToken(String client_id, String nonce) throws AppException, Exception {
		log.info("buildAccessToken called");
		log.info("buildAccessToken - /token got client_id: {}", client_id);
		log.info("buildAccessToken - /token got nonce: {}", nonce);

		String accessToken = null;

		Client client = clientService.getClient(client_id);
		String applicationId = client.getApplicationId();
		String applicationName = client.getApplicationName();
		String applicationUrl = client.getApplicationUrl();
		accessToken = AccessTokenMapper.buildTokenForClientCredentialGrantType(client_id, applicationId, applicationName, applicationUrl, nonce);

		return accessToken;
	}

	public String buildAccessToken(String client_id, String theUsersAuthorizationCode, String nonce) throws Exception, AppException {
		log.info("buildAccessToken called");
		log.info("buildAccessToken - /token got code: {}", theUsersAuthorizationCode);
		log.info("buildAccessToken - /token got nonce: {}", nonce);
		log.info("buildAccessToken - /token got client_id: {}", client_id);
		UserAuthorization userAuthorization = authorizationService.getAuthorization(theUsersAuthorizationCode);
		if (userAuthorization == null) {
			log.info("The authorization code not found");
			throw AppExceptionCode.AUTHORIZATIONCODE_NOTFOUND_8000;
		} else {
			return buildAccessToken(client_id, userAuthorization.getUserTokenId(), userAuthorization.getScopes(), nonce);
		}
	}

	private String findApplicationId(String clientId) {
		return ClientIDUtil.getApplicationId(clientId);
	}


	public String buildCode() {
		return UUID.randomUUID().toString();
	}


	public String refreshAccessToken(String client_id, String refresh_token, String nonce) throws Exception, AppException {
		try {
			log.info("refreshAccessToken called");
			log.info("refreshAccessToken - /token got refresh_token: {}", refresh_token);
			log.info("refreshAccessToken - /token got client_id: {}", client_id);
			log.info("refreshAccessToken - /token got nonce: {}", nonce);

			String decrypt = ClientIDUtil.decrypt(refresh_token);
			if (decrypt == null) {
				throw AppExceptionCode.REFRESHTOKEN_INVALID_8005;
			}
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
			Map<String, Set<String>> jwtRolesByScope = client.getJwtRolesByScope();
			return AccessTokenMapper.buildToken(userToken, client_id, applicationId, applicationName, applicationUrl, nonce, authorizationService.buildScopes(scopeList), jwtRolesByScope);
		} catch (Exception e) {
			log.error("Unable to refresh accessToken: ", e);
			throw e;
		}
	}
}
