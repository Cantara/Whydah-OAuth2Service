package net.whydah.service.authorizations;

import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.whydah.commands.config.ConstantValues;
import net.whydah.service.CredentialStore;
import net.whydah.sso.commands.adminapi.user.CommandGetUser;
import net.whydah.sso.commands.adminapi.user.role.CommandAddUserRole;
import net.whydah.sso.commands.adminapi.user.role.CommandGetUserRoles;
import net.whydah.sso.commands.adminapi.user.role.CommandUpdateUserRole;
import net.whydah.sso.commands.userauth.CommandGetUserTokenByUserTicket;
import net.whydah.sso.commands.userauth.CommandGetUserTokenByUserTokenId;
import net.whydah.sso.commands.userauth.CommandRefreshUserToken;
import net.whydah.sso.commands.userauth.CommandReleaseUserToken;
import net.whydah.sso.session.WhydahApplicationSession2;
import net.whydah.sso.user.mappers.UserRoleMapper;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import net.whydah.sso.user.types.UserToken;
import net.whydah.util.URLHelper;

/**
 * Created by baardl on 13.08.17.
 */
@Service
public class UserAuthorizationService {
	private static final Logger log = getLogger(UserAuthorizationService.class);
	public static final String DEVELOPMENT_USER_TOKEN_ID = "345460b3-c93e-4150-9808-c62facbadd99";
	protected static final ObjectMapper mapper = new ObjectMapper();

	protected UserAuthorizationsRepository authorizationsRepository;
	protected SSOUserSessionRepository ssoUserSessionRepository;
	protected CredentialStore credentialStore;
	protected String ROLE_NAME_FOR_SCOPES = "OpenIDConnect";

	@Autowired
	public UserAuthorizationService(UserAuthorizationsRepository authorizationsRepository, SSOUserSessionRepository ssoUserSessionRepository, CredentialStore credentialStore) {
		this.authorizationsRepository = authorizationsRepository;
		this.ssoUserSessionRepository = ssoUserSessionRepository;
		this.credentialStore = credentialStore;
	}

	public void addAuthorization(UserAuthorizationSession userAuthorization) {
		authorizationsRepository.addAuthorization(userAuthorization);
	}

	public void addSSOSession(OAuthenticationSession session) {
		ssoUserSessionRepository.addSession(session);
	}

	public OAuthenticationSession getSSOSession(String sessionId) {
		return ssoUserSessionRepository.getSession(sessionId);
	}

	public Response toSSO(String client_id, String scope, String response_type, String response_mode, String state, String nonce, String redirect_uri, String logged_in_users, String referer_channel, String code_challenge, String code_challenge_method) {
		OAuthenticationSession session = new OAuthenticationSession(scope, response_type, response_mode, client_id, redirect_uri, state, nonce, code_challenge, code_challenge_method, logged_in_users, referer_channel, new Date());
		addSSOSession(session);
		String directUri = UriComponentsBuilder
				.fromUriString(ConstantValues.MYURI.replaceFirst("/$", "") + "/user")
				.queryParam("oauth_session", session.getId())
				.build().toUriString();

		URI login_redirect = URI.create(ConstantValues.SSO_URI.replaceFirst("/$", "") + "/login?redirectURI=" + URLHelper.encode(directUri));
		return Response.status(Response.Status.MOVED_PERMANENTLY).location(login_redirect).build();
	}


	public Map<String, Object> buildUserModel(String clientId, String clientName, String scope, String response_type, String response_mode, String state, String nonce, String redirect_uri, String userTokenIdFromCookie, String referer_channel, String code_challenge, String code_challenge_method) {
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
			name = userToken.getFirstName();
			if(userToken.getLastName()!=null) {
				name += " " + userToken.getLastName();
			}
		}

		user.put("name", name);

		Map<String, Object> model = new HashMap<>();
		model.put("user", user);

		model = addParameter("client_id", clientId, model);
		model = addParameter("client_name", clientName, model);
		model = addParameter("scope", scope, model);
		model = addParameter("response_type", response_type, model);
		model = addParameter("response_mode", response_mode, model);
		model = addParameter("state", state, model);
		model = addParameter("redirect_uri", redirect_uri, model);
		model = addParameter("customer_ref", userToken.getPersonRef(), model);
		model = addParameter("usertoken_id", userTokenIdFromCookie, model);
		model = addParameter("nonce", nonce, model);
		model = addParameter("referer_channel", referer_channel, model);
		model = addParameter("code_challenge", code_challenge, model);
		model = addParameter("code_challenge_method", code_challenge_method, model);
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

	public UserAuthorizationSession getAuthorization(String theUsersAuthorizationCode) {
		return authorizationsRepository.getAuthorization(theUsersAuthorizationCode);
	}

	public String findUserIdFromUserAuthorization(String theUsersAuthorizationCode) {
		UserAuthorizationSession userAuthorization = authorizationsRepository.getAuthorization(theUsersAuthorizationCode);
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
		log.info("Attempting to lookup usertokenId:" + userTokenId);
		String userTokenXml = "";
		try {
			UserToken userToken = null;
			WhydahApplicationSession2 was = credentialStore.getWas();
			URI tokenServiceUri = URI.create(was.getSTS());
			String oauth2proxyTokenId = was.getActiveApplicationTokenId();
			log.info("Attempting to lookup oauth2proxyTokenId:" + oauth2proxyTokenId);
			String oauth2proxyAppTokenXml = was.getActiveApplicationTokenXML();
			log.info("Attempting to lookup oauth2proxyAppTokenXml:" + oauth2proxyAppTokenXml.replace("\n", ""));
			log.info("Attempting to lookup (get_usertoken_by_usertokenid) tokenServiceUri:" + tokenServiceUri);
			userTokenXml = new CommandGetUserTokenByUserTokenId(tokenServiceUri, oauth2proxyTokenId, oauth2proxyAppTokenXml, userTokenId).execute();
			if(userToken ==null) {
				//try to renew 

			}

			if(userTokenXml!=null) {
				log.info("==> Got lookup userTokenXml:" + userTokenXml.replace("\n", ""));
				userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);
				log.info("Got userToken:" + userToken);
				return userToken;
			} else {
				return null;
			}
		} catch (Exception e) {
			log.warn("Unable to parse userTokenXml returned from sts: " + userTokenXml.replace("\n", "") + "", e);
			return null;
		}

		//see UserTokenXpathHelper
	}

	public UserToken refreshUserTokenFromUserTokenId(String userTokenId) {
		log.info("Attempting to refresh usertoken by userTokenId:" + userTokenId);
		String userTokenXml = "";
		try {
			UserToken userToken = null;
			WhydahApplicationSession2 was = credentialStore.getWas();
			URI tokenServiceUri = URI.create(was.getSTS());
			log.info("Attempting to refresh usertoken tokenServiceUri:" + tokenServiceUri);
			String oauth2proxyTokenId = was.getActiveApplicationTokenId();
			log.info("Attempting to refresh usertoken oauth2proxyTokenId:" + oauth2proxyTokenId);
			String oauth2proxyAppTokenXml = was.getActiveApplicationTokenXML();
			log.info("Attempting to refresh usertoken (refresh_usertoken) oauth2proxyAppTokenXml:" + oauth2proxyAppTokenXml.replace("\n", ""));
			userTokenXml = new CommandRefreshUserToken(tokenServiceUri, oauth2proxyTokenId, oauth2proxyAppTokenXml, userTokenId).execute();
			if (userTokenXml == null || userTokenXml.length() < 10) {
				log.warn("Unable to renew user-token. Has the user-session expored already?");
			} else {
				log.info("==> Got refresh userTokenXml:" + userTokenXml.replace("\n", ""));
			}
			userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);
			log.info("Got userToken:" + userToken);
			return userToken;
		} catch (Exception e) {
			log.warn("Unable to parse userTokenXml returned from sts: \"" + userTokenXml + "\"", e);
			//            log.warn("Unable to parse userTokenXml returned from sts: " + userTokenXml.replace("\n", "") + "", e);
			return null;
		}
		//see UserTokenXpathHelper
	}

	public UserToken findUserTokenFromUserTicket(String ticket) {
		log.info("Attempting to lookup usertoken by ticket:" + ticket);
		String userTokenXml = "";
		try {
			UserToken userToken = null;
			WhydahApplicationSession2 was = credentialStore.getWas();
			URI tokenServiceUri = URI.create(was.getSTS());
			String oauth2proxyTokenId = was.getActiveApplicationTokenId();
			String oauth2proxyAppTokenXml = was.getActiveApplicationTokenXML();
			userTokenXml = new CommandGetUserTokenByUserTicket(tokenServiceUri, oauth2proxyTokenId, oauth2proxyAppTokenXml, ticket).execute();
			userToken = UserTokenMapper.fromUserTokenXml(userTokenXml);
			return userToken;
		} catch (Exception e) {
			log.warn("Unable to parse userTokenXml returned from sts " + userTokenXml + "", e);
			return null;
		}

		//see UserTokenXpathHelper
	}

	public void saveScopesToWhydahRoles(UserToken ut, List<String> scopes)  {
		try {
			List<UserApplicationRoleEntry> roles = ut.getRoleList();
			UserApplicationRoleEntry found = null;
			for(UserApplicationRoleEntry role: roles) {
				if(role.getApplicationId().equals(credentialStore.getWas().getMyApplicationCredential().getApplicationID())
						&& role.getRoleName().equalsIgnoreCase(ROLE_NAME_FOR_SCOPES)
						) {
					found = role;
					break;
				} 
			}

			String newRoleValue = mapper.writeValueAsString(scopes);

			if(found!=null) {
				log.info("saveScopesToWhydahRoles: role found, updating new scopes value {}", newRoleValue);
				found.setRoleValue(newRoleValue);
				
				CommandUpdateUserRole roleCmd = new CommandUpdateUserRole(URI.create(credentialStore.getWas().getUAS()), 
						credentialStore.getActiveApplicationTokenId(), 
						credentialStore.getAdminUserTokenId(), 
						ut.getUid(), 
						found.getId(), UserRoleMapper.toJson(found));
				String result = roleCmd.execute();
				if(result!=null) {
					log.info("saveScopesToWhydahRoles: update role succeeded, response {}", result);
				} else {
					log.error("saveScopesToWhydahRoles: update role failed");
				}
			} else {
				log.info("saveScopesToWhydahRoles: role not found, createing new role with the scopes value {}", newRoleValue);
				UserApplicationRoleEntry roleEn = new UserApplicationRoleEntry();
				roleEn.setApplicationId(credentialStore.getWas().getMyApplicationCredential().getApplicationID());
				roleEn.setApplicationName(credentialStore.getWas().getMyApplicationCredential().getApplicationName());
				roleEn.setId(UUID.randomUUID().toString());
				roleEn.setOrgName("Whydah");
				roleEn.setRoleName(ROLE_NAME_FOR_SCOPES);
				roleEn.setUserId(ut.getUid());
				roleEn.setRoleValue(newRoleValue);
			
				//create process
				CommandAddUserRole roleCmd = new CommandAddUserRole(URI.create(credentialStore.getWas().getUAS()), 
						credentialStore.getActiveApplicationTokenId(), 
						credentialStore.getAdminUserTokenId(), 
						ut.getUid(), 
						UserRoleMapper.toJson(roleEn));
				String result = roleCmd.execute();
				if(result!=null) {
					log.info("saveScopesToWhydahRoles: create role succeeded, response {}", result);
				} else {
					log.error("saveScopesToWhydahRoles: create role failed");
				}

			}
		}catch(Exception ex) {
			ex.printStackTrace();
			log.error("saveScopesToWhydahRoles failed due to the exception {}", ex);
		}


	}

	public void releaseUserToken(String userTokenId) {
		log.info("Attempting to lookup usertokenId:" + userTokenId);
		try {
			WhydahApplicationSession2 was = credentialStore.getWas();
			URI tokenServiceUri = URI.create(was.getSTS());
			String oauth2proxyTokenId = was.getActiveApplicationTokenId();
			log.info("Attempting to lookup oauth2proxyTokenId:" + oauth2proxyTokenId);
			String oauth2proxyAppTokenXml = was.getActiveApplicationTokenXML();
			log.info("Attempting to lookup oauth2proxyAppTokenXml:" + oauth2proxyAppTokenXml.replace("\n", ""));
			log.info("Attempting to lookup (get_usertoken_by_usertokenid) tokenServiceUri:" + tokenServiceUri);
			boolean result = new CommandReleaseUserToken(tokenServiceUri, oauth2proxyTokenId, oauth2proxyAppTokenXml, userTokenId).execute();
			log.info("Got logout result:" + result);
		} catch (Exception e) {
			log.warn("Unexpected exception {}", e);
		}

		//see UserTokenXpathHelper
	}
}
