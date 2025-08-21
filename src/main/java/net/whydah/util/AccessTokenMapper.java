package net.whydah.util;

import static org.slf4j.LoggerFactory.getLogger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;

import io.jsonwebtoken.Claims;
import net.whydah.commands.config.ConstantValues;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import net.whydah.sso.user.types.UserToken;

/**
 * Created by baardl on 15.08.17.
 * Modified by huy on 12.06.20
 * Upgrade to support OpenID Connect
 */
public class AccessTokenMapper {
    private static final Logger log = getLogger(AccessTokenMapper.class);

    //Within the OpenID Connect specification, the standard scopes are defined as openid, profile, email, address, and phone
    //If you use any scope beyond those, you’re beyond the OIDC specification and back into general OAuth and this is where it gets complicated.
    public static final String SCOPE_OPENID = "openid";
    public static final String SCOPE_PROFILE = "profile"; //a lack of support for a profile url (for example: https://fb.com/me) in UserToken, but can possibly define it as a usertoken's role
    public static final String SCOPE_EMAIL = "email";
    public static final String SCOPE_ADDRESS = "address"; //we lack this field in UserToken
    public static final String SCOPE_PHONE = "phone";

    public static Set<String> getWhitelistedRolePatternsForScope(List<String> scopes, Map<String, Set<String>> jwtRolesByScope) {
        Set<String> patternUnion = new LinkedHashSet<>();
        for (String scope : scopes) {
            log.debug("getWhitelistedRolePatternsForScope - Looping scope:" + scope);
            Set<String> patterns = jwtRolesByScope.get(scope);
            log.debug("getWhitelistedRolePatternsForScope - Found  patterns:" + patterns);
            if (patterns != null) {
                for (String pattern : patterns) {
                    log.debug("getWhitelistedRolePatternsForScope - whitelistPatterns - Adding pattern:" + pattern);
                    patternUnion.add(pattern.toLowerCase());
                }
            }
        }
        return patternUnion;
    }

    public static boolean isRoleInWhitelistForScope(Set<String> whitelistPatterns, UserApplicationRoleEntry roleEntry) {
        String roleName = roleEntry.getRoleName().toLowerCase();
        log.debug("isRoleInWhitelistForScope - checking roleName:" + roleName);

        if (whitelistPatterns.contains(roleName)) {
            log.debug("isRoleInWhitelistForScope - whitelistPatterns.contains roleName:" + roleName + " - return true");
            return true;
        }
        for (String pattern : whitelistPatterns) {
            log.debug("isRoleInWhitelistForScope - checking pattern:" + pattern);
            if (pattern.endsWith("*")) {
                String prefix = pattern.substring(0, pattern.length() - 1);
                log.debug("isRoleInWhitelistForScope - Checking if rolename:" + roleName + " startsWith:" + prefix);
                if (roleName.startsWith(prefix)) {
                    log.debug("isRoleInWhitelistForScope - roleName.startsWith(prefix):" + prefix + " - return true");
                    return true;
                }
            }
        }
        log.debug("isRoleInWhitelistForScope - checking roleName:" + roleName + " - return false");
        return false;
    }

    public static Map<String, Object> getClaimsForUserRoles(String applicationId, List<String> userAuthorizedScope, List<UserApplicationRoleEntry> roleList, Map<String, Set<String>> jwtRolesByScope) {
        Map<String, Object> claims = new LinkedHashMap<>();
        Set<String> rolePatterns = getWhitelistedRolePatternsForScope(userAuthorizedScope, jwtRolesByScope);
        for (UserApplicationRoleEntry userApplicationRoleEntry : roleList) {
        	//if (userApplicationRoleEntry.getApplicationId().equalsIgnoreCase(applicationId) && isRoleInWhitelistForScope(rolePatterns, userApplicationRoleEntry)) {
            log.debug("Parsing:" + userApplicationRoleEntry.getRoleName() + " checking isRoleInWhitelistForScope(rolePatterns, userApplicationRoleEntry) - rolepatterns:" + rolePatterns);
        	if (isRoleInWhitelistForScope(rolePatterns, userApplicationRoleEntry)) {
                log.debug("claims.put:" + userApplicationRoleEntry.getRoleName(), userApplicationRoleEntry.getRoleValue());
                claims.put("role_" + userApplicationRoleEntry.getRoleName(), userApplicationRoleEntry.getRoleValue());
        	}
        }
        return claims;
    }

    public static String buildToken(UserToken userToken, String clientId, String applicationId, String applicationName, String applicationUrl, String nonce, List<String> userAuthorizedScope, Map<String, Set<String>> jwtRolesByScope, String code) {
        String result = null;
        if (userToken != null) {

            long expireSec = (Long.valueOf(userToken.getLifespan()) / 1000);
            expireSec = expireSec - 30; //processing time on STS for validating a usertoken
            if (expireSec <= 0) {
                expireSec = expireSec - 5; //too small, maybe 5 secs
            }


            if (nonce == null) {
                nonce = "";
            }

            Date expiration = new Date(System.currentTimeMillis() + expireSec * 1000);

            String accessToken = buildAccessToken(userToken, clientId, applicationId, applicationName, applicationUrl, nonce, userAuthorizedScope, jwtRolesByScope, expiration);
            
            JsonObjectBuilder tokenBuilder = Json.createObjectBuilder()
                    .add("access_token", accessToken) //this client will use this to access other servers' resources
                    .add("token_type", "bearer")
                    .add("nonce", nonce)
                    .add("expires_in", expireSec) //in seconds
                    .add("refresh_token", ClientIDUtil.encrypt(userToken.getUserTokenId() + ":" + String.join(" ", userAuthorizedScope)));

            if (userAuthorizedScope.contains(SCOPE_OPENID)) {
                //OpenID Connect requires "id_token"
            	String at_hash = null;
				try {
					at_hash = get_at_hash(accessToken);
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				}
            	String c_hash = null;
            	if(code!=null) {
            		try {
						c_hash = get_c_hash(code);
					} catch (NoSuchAlgorithmException e) {
						e.printStackTrace();
					}
            	}
            	
                tokenBuilder = tokenBuilder
                		.add("id_token", buildClientToken(userToken, clientId, applicationId, applicationName, applicationUrl, nonce, userAuthorizedScope, jwtRolesByScope, expiration, c_hash, at_hash)) //attach granted scopes to JWT
                		;
                        //.add("nonce", nonce);

            } 
            //else {
//                //back to general OAuth
//                tokenBuilder = buildUserInfoJson(tokenBuilder, userToken, applicationId, userAuthorizedScope);
//            }
            result = tokenBuilder.build().toString();
        }
        log.info("token built: {}", result);
        return result;
    }

    private static String get_c_hash(String code) throws NoSuchAlgorithmException {
    	MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] asciiValue = code.getBytes(StandardCharsets.US_ASCII);
	    byte[] encodedHash = md.digest(asciiValue);
	    byte[] halfOfEncodedHash = Arrays.copyOf(encodedHash, (encodedHash.length / 2));
	    return Base64.getUrlEncoder().withoutPadding().encodeToString(halfOfEncodedHash);
	}

	private static String get_at_hash(String accessToken) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] asciiValue = accessToken.getBytes(StandardCharsets.US_ASCII);
	    byte[] encodedHash = md.digest(asciiValue);
	    byte[] halfOfEncodedHash = Arrays.copyOf(encodedHash, (encodedHash.length / 2));
	    return Base64.getUrlEncoder().withoutPadding().encodeToString(halfOfEncodedHash);
	}

	public static String buildTokenForClientCredentialGrantType(String clientId, String applicationId, String applicationName, String applicationUrl, long appLifespanInSeconds, String appToken, String nonce) throws Exception {
        String accessToken = null;
        if (nonce == null) {
            nonce = "";
        }

        if(appLifespanInSeconds == 0)  {
        	appLifespanInSeconds = ConstantValues.DF_JWT_LIFESPAN;
        }
        long exp =  (appLifespanInSeconds - 30) ;
        Date expiration = new Date(System.currentTimeMillis() + exp* 1000);

        JsonObjectBuilder tokenBuilder = Json.createObjectBuilder()
                .add("access_token", buildAccessTokenForClientCredetntialGrantType(clientId, applicationId, applicationName, applicationUrl, appToken, nonce, expiration))//this client will use this to access other servers' resources
                .add("token_type", "bearer")
                .add("nonce", nonce)
                .add("expires_in", exp);


        accessToken = tokenBuilder.build().toString();

        log.info("buildTokenForClientCredentialGrantType built: {}", accessToken);
        return accessToken;
    }

//    public static JsonObjectBuilder buildUserInfoJson(JsonObjectBuilder tokenBuilder, UserToken userToken, String applicationId, List<String> userAuthorizedScope) {
//        if (userAuthorizedScope.contains(SCOPE_EMAIL)) {
//            tokenBuilder = tokenBuilder.add(SCOPE_EMAIL, userToken.getEmail());
//        }
//        if (userAuthorizedScope.contains(SCOPE_PHONE)) {
//            tokenBuilder = tokenBuilder.add(SCOPE_PHONE, userToken.getCellPhone());
//        }
//        
//        getClaimsForUserRoles(applicationId, userAuthorizedScope, userToken.getRoleList(), )
//        tokenBuilder = buildRoles(userToken.getRoleList(), applicationId, userAuthorizedScope, tokenBuilder);
//        return tokenBuilder;
//    }

    private static String buildClientToken(UserToken userToken, String clientId, String applicationId, String applicationName, String applicationUrl, String nonce, List<String> userAuthorizedScope, Map<String, Set<String>> jwtRolesByScope, Date expiration, String c_hash, String at_hash) {
        if (nonce == null) {
            nonce = "";
        }

        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put(Claims.ID, UUID.randomUUID().toString());
        claims.put(Claims.SUBJECT, userToken.getUserName());
        claims.put(Claims.AUDIENCE, clientId);
        claims.put("app_url", applicationUrl != null ? applicationUrl : applicationName);
        claims.put(Claims.ISSUER, ConstantValues.MYURI);
        claims.put("nonce", nonce);    
        claims.put("given_name", userToken.getFirstName());
        claims.put("family_name", userToken.getLastName());
        claims.put("customer_ref", userToken.getPersonRef());
        claims.put("usertoken_id", userToken.getUserTokenId()); //used by other back-end service
        claims.put("security_level", userToken.getSecurityLevel());
        
        if(c_hash!=null) {
        	claims.put("c_hash", c_hash);
        }
        if(at_hash!=null) {
        	claims.put("at_hash", at_hash);
        }
        if (userAuthorizedScope.contains(SCOPE_EMAIL)) {
            claims.put(SCOPE_EMAIL, userToken.getEmail());
        }
        if (userAuthorizedScope.contains(SCOPE_PHONE)) {
            claims.put(SCOPE_PHONE, userToken.getCellPhone());
        }
        
        Map<String, Object> roles = getUserAppRoles(userToken, applicationId, userAuthorizedScope, jwtRolesByScope);        
        claims.putAll(roles);

        return JwtUtils.generateRSAJwtToken(claims, expiration);
    }

	public static Map<String, Object> getUserAppRoles(UserToken userToken, String applicationId,
			List<String> userAuthorizedScope, Map<String, Set<String>> jwtRolesByScope) {
		
		//add any allowed roles configured in apptags
        Map<String, Object> roles = getClaimsForUserRoles(applicationId, userAuthorizedScope, userToken.getRoleList(), jwtRolesByScope);
        log.debug("getClaimsForUserRoles returns {} roles " + roles.size());
        //override custom data role for this specific app
        for (UserApplicationRoleEntry role : userToken.getRoleList()) {
            log.debug("userrole found {}", role.toJson() + " - checking is role.getApplicationId():" + role.getApplicationId() + " equals: " + applicationId);
            if (role.getApplicationId().equals(applicationId)) {
            	log.debug("app match found name {}, id {}", role.getApplicationName(), role.getApplicationId());
            	
                if (userAuthorizedScope.stream().anyMatch(i -> i.equalsIgnoreCase(role.getRoleName()))) {
                    roles.put("role_" + role.getRoleName(), role.getRoleValue());
                }
            }
        }
		return roles;
	}

    public static String buildAccessToken(UserToken usertoken, String clientId, String appId, String applicationName, String applicationUrl, String nonce, List<String> userAuthorizedScope, Map<String, Set<String>> jwtRolesByScope, Date expiration) {
        if (nonce == null) {
            nonce = "";
        }
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put(Claims.ID, UUID.randomUUID().toString());
        claims.put(Claims.SUBJECT, usertoken.getUserName());  //a locally unique identity in the context of the issuer. The processing of this claim is generally application specific
        claims.put(Claims.AUDIENCE, clientId);
        claims.put("app_url", applicationUrl != null ? applicationUrl : applicationName);
        claims.put("nonce", nonce);
//		claims.put(Claims.AUDIENCE, applicationUrl);
        claims.put(Claims.ISSUER, ConstantValues.MYURI);
        //useful info for back-end services
        claims.put("app_id", appId); //used by other back-end services
        claims.put("app_name", applicationName); //used by other back-end services
        claims.put("customer_ref", usertoken.getPersonRef()); //used by other back-end services
        claims.put("usertoken_id", usertoken.getUserTokenId()); //used by other back-end services
        claims.put("security_level", usertoken.getSecurityLevel());   
        claims.put("scope", String.join(" ", userAuthorizedScope));  //used for /userinfo endpoint, re-populating user info with this granted scope list
 //       claims.putAll(getClaimsForUserRoles(appId, userAuthorizedScope, usertoken.getRoleList(), jwtRolesByScope));
        return JwtUtils.generateRSAJwtToken(claims, expiration);
    }

    public static String buildAccessTokenForClientCredetntialGrantType(String clientId, String appId, String applicationName, String applicationUrl, String appToken, String nonce, Date expiration) {
        if (nonce == null) {
            nonce = "test";
        }

        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put(Claims.ID, UUID.randomUUID().toString());
        claims.put(Claims.SUBJECT, clientId);  //a locally unique identity in the context of the issuer. The processing of this claim is generally application specific
        claims.put(Claims.AUDIENCE, clientId);
        claims.put("app_url", applicationUrl != null ? applicationUrl : applicationName);
        claims.put("nonce", nonce);
        // claims.put(Claims.AUDIENCE, applicationUrl != null ? applicationUrl : applicationName);
        claims.put(Claims.ISSUER, ConstantValues.MYURI);
        //useful info for back-end services
        claims.put("app_id", appId); //used by other back-end services
        claims.put("app_token", appToken); //used by other back-end services
        claims.put("app_name", applicationName); //used by other back-end services
        return JwtUtils.generateRSAJwtToken(claims, expiration);
    }


//    protected static JsonObjectBuilder buildRoles(List<UserApplicationRoleEntry> roleList, String applicationId, List<String> userAuthorizedScope, JsonObjectBuilder tokenBuilder) {
//        for (UserApplicationRoleEntry role : roleList) {
//            if (role.getApplicationId().equals(applicationId)) {
//                if (userAuthorizedScope.contains(role.getRoleName())) {
//                    tokenBuilder.add(role.getRoleName(), role.getRoleValue());
//                }
//            }
//        }
//        return tokenBuilder;
//    }
}
