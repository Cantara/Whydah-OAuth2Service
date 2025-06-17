package net.whydah.service.oauth2proxyserver;

import io.jsonwebtoken.Claims;
import net.whydah.commands.config.ConstantValues;
import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.service.authorizations.UserAuthorizationSession;
import net.whydah.service.clients.Client;
import net.whydah.service.clients.ClientService;
import net.whydah.service.clients.CodeChallengeMethod;
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

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.net.URI;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by baardl on 09.08.17.
 */
@Service
public class TokenService {
	private static final Logger log = getLogger(TokenService.class);

	private final UserAuthorizationService authorizationService;

	private final ClientService clientService;

	//@Inject
	@Autowired
	public TokenService(UserAuthorizationService authorizationService, ClientService clientService) {
		this.authorizationService = authorizationService;
		this.clientService = clientService;
	}


	public String buildToken(String client_id, String client_secret, String grant_type, String code, String nonce, String redirect_uri, String refresh_token, String username, String password, String code_verifier) throws Exception, AppException {

		log.info("TokenService - /token got grant_type: {}", grant_type);
		log.info("buildAccessToken - /token got client_id: {}", client_id);
		log.info("buildAccessToken - /token got nonce: {}", nonce);

		String accessToken = null;
		boolean isClientIdValid = clientService.isClientValid(client_id);
		if (isClientIdValid) {
			log.info("TokenService - isClientIdValid: {}", isClientIdValid);
			accessToken = createAccessToken(client_id, client_secret, grant_type, code, refresh_token, username, password, nonce, code_verifier);
		} else {
			log.info("TokenService - isClientIdValid: {}", isClientIdValid);
			throw AppExceptionCode.CLIENT_NOTFOUND_8002;
		}
		log.warn("TokenService - no Whydah - dummy standalone fallback");
		return accessToken;
	}

	protected String createAccessToken(String client_id, String client_secret, String grant_type, String code, String refresh_token, String username, String password, String nonce, String code_verifier) throws Exception, AppException {

		log.info("TokenService - createAccessToken -grant type:" + grant_type);
		log.info("createAccessToken - /token got nonce: {}", nonce);

		String accessToken = null;
		if ("client_credentials".equalsIgnoreCase(grant_type)) {
			log.info("TokenService - createAccessToken - client_credentials");
			accessToken = buildAccessTokenForA2P(client_id, client_secret, nonce);
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
			accessToken = buildAccessToken(client_id, ut, authorizationService.buildScopes("openid profile phone email"), nonce, code);
		}
		if ("authorization_code".equalsIgnoreCase(grant_type)) {
			log.info("TokenService - createAccessToken - authorization_code");
			UserAuthorizationSession uauth = authorizationService.getAuthorization(code);
			if(uauth==null) {
				throw AppExceptionCode.AUTHORIZATIONCODE_NOTFOUND_8000;
			}
			if(uauth.getCodeChallenge()!=null && uauth.getCodeChallenge().length()>0) {
				try {
		            CodeChallengeMethod codeChallengeMethod = Optional.ofNullable(uauth.getCodeChallengeMethod())
		                    .map(String::toUpperCase)
		                    .map(CodeChallengeMethod::valueOf)
		                    .orElse(CodeChallengeMethod.PLAIN);
		            if (codeChallengeMethod == CodeChallengeMethod.NONE) {
		            	accessToken = buildAccessToken(client_id, code, nonce, uauth);
		            } else if(codeChallengeMethod.transform(code_verifier).equals(uauth.getCodeChallenge())) {
		            	accessToken = buildAccessToken(client_id, code, nonce, uauth);
		            } else {
		                throw AppExceptionCode.CODEVERIFIER_INVALID_8009;
		            }
		            
		        } catch (IllegalArgumentException e) {
		            throw AppExceptionCode.CODECHALLENGEMETHOD_NOTSUPPORTED_8008;
		        }
			} else {
				accessToken = buildAccessToken(client_id, code, nonce, uauth);
			}
			
		} else if ("refresh_token".equalsIgnoreCase(grant_type)) {
			log.info("TokenService - createAccessToken - refresh_token");
			accessToken = refreshAccessToken(client_id, refresh_token, nonce);
		}
		log.info("TokenService - createAccessToken - accessToken:" + accessToken);
		return accessToken;
	}

	public String buildAccessToken(String client_id, String usertokenId, List<String> userAuthorizedScopes, String nonce, String code) throws AppException {
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
			accessToken = AccessTokenMapper.buildToken(userToken, client_id, applicationId, applicationName, applicationUrl, nonce, userAuthorizedScopes, jwtRolesByScope, code);
		} else {
			throw AppExceptionCode.USERTOKEN_INVALID_8001;
		}


		return accessToken;
	}

	public String buildAccessToken(String client_id, UserToken usertoken, List<String> userAuthorizedScopes, String nonce, String code) throws AppException {
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
			accessToken = AccessTokenMapper.buildToken(usertoken, client_id, applicationId, applicationName, applicationUrl, nonce, userAuthorizedScopes, jwtRolesByScope, code);
		} else {
			throw AppExceptionCode.USERTOKEN_INVALID_8001;
		}
		return accessToken;
	}

	public String buildAccessTokenForA2P(String client_id, String secret, String nonce) throws AppException, Exception {
		log.info("buildAccessToken called");
		log.info("buildAccessToken - /token got client_id: {}", client_id);
		log.info("buildAccessToken - /token got nonce: {}", nonce);

		String accessToken = null;

		Client client = clientService.getClient(client_id);
		String applicationId = client.getApplicationId();
		String applicationName = client.getApplicationName();
		String applicationUrl = client.getApplicationUrl();
		Application app = clientService.getApplicationByClientId(client_id);
		long appLifespan = app.getSecurity().getMaxSessionTimeoutSeconds();
		
		if(!secret.equals(app.getSecurity().getSecret())) {
			throw AppExceptionCode.SECRET_INVALID_8006;
		}
		
		String myAppTokenXml = new CommandLogonApplication(URI.create(ConstantValues.STS_URI), new ApplicationCredential(app.getId(), app.getName(), app.getSecurity().getSecret())).execute();
		
		if(myAppTokenXml==null) {
			throw AppExceptionCode.APPLOGON_FAILED_8007;
		}
		
		String applicationTokenID = ApplicationXpathHelper.getAppTokenIdFromAppTokenXml(myAppTokenXml);
		accessToken = AccessTokenMapper.buildTokenForClientCredentialGrantType(client_id, applicationId, applicationName, applicationUrl, appLifespan, applicationTokenID, nonce);

		return accessToken;
	}

	public String buildAccessToken(String client_id, String theUsersAuthorizationCode, String nonce, UserAuthorizationSession userAuthorizationSession) throws Exception, AppException {
		log.info("buildAccessToken called");
		log.info("buildAccessToken - /token got code: {}", theUsersAuthorizationCode);
		log.info("buildAccessToken - /token got nonce: {}", nonce);
		log.info("buildAccessToken - /token got client_id: {}", client_id);
		
		if (userAuthorizationSession == null) {
			log.info("The authorization code not found");
			throw AppExceptionCode.AUTHORIZATIONCODE_NOTFOUND_8000;
		} else {
			return buildAccessToken(client_id, userAuthorizationSession.getUserTokenId(), userAuthorizationSession.getScopes(), nonce, userAuthorizationSession.getCode());
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
			return AccessTokenMapper.buildToken(userToken, client_id, applicationId, applicationName, applicationUrl, nonce, authorizationService.buildScopes(scopeList), jwtRolesByScope, null);
		} catch (Exception e) {
			log.error("Unable to refresh accessToken: ", e);
			throw e;
		}
	}


	public JsonObjectBuilder buildUserInfo(Claims accessTokenClaims) {
		UserToken userToken = authorizationService.findUserTokenFromUserTokenId(accessTokenClaims.get("usertoken_id", String.class));
		if (userToken == null) {
			return null;
		}
		String scope = accessTokenClaims.get("scope", String.class);
		log.debug("user scope {}", scope);
		Set<String> clientIds = accessTokenClaims.getAudience();
		
		JsonObjectBuilder tokenBuilder = Json.createObjectBuilder()
				.add("uid", userToken.getUid())
				.add("sub", accessTokenClaims.getSubject())
				.add("given_name", userToken.getFirstName())
				.add("family_name", userToken.getLastName())
				.add("customer_ref", userToken.getPersonRef())
				.add("first_name", userToken.getFirstName()) //support for old version
				.add("last_name", userToken.getLastName()) //support for old version
				.add("security_level", userToken.getSecurityLevel())
				.add("last_seen", userToken.getLastSeen())				
				.add("scope", scope)				
				;	
		if (scope.contains(AccessTokenMapper.SCOPE_EMAIL)) {
			tokenBuilder = tokenBuilder.add(AccessTokenMapper.SCOPE_EMAIL, userToken.getEmail());
		}
		if (scope.contains(AccessTokenMapper.SCOPE_PHONE)) {
			tokenBuilder = tokenBuilder.add(AccessTokenMapper.SCOPE_PHONE, userToken.getCellPhone());
			
			//support for old version
			tokenBuilder = tokenBuilder.add("phone_number", userToken.getCellPhone());
		}

		String clientApplicationTokenId = "";
		Map<String, Set<String>> clientJwtRolesByScope = null;
		for (String clientId : clientIds) {
			Client client = clientService.getClient(clientId);
			if (client != null) {

				if (client.getApplicationUrl() != null) {
					clientApplicationTokenId = client.getApplicationId();
					clientJwtRolesByScope = client.getJwtRolesByScope();
				}
			}
		}
		//See README apptags
		Map<String, Object> roleMap = AccessTokenMapper.getUserAppRoles(userToken, clientApplicationTokenId, authorizationService.buildScopes(scope), clientJwtRolesByScope);
		tokenBuilder.add("roles", Json.createObjectBuilder(roleMap));
	    return tokenBuilder;
	}
}
