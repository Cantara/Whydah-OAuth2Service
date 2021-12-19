package net.whydah.util;

import io.jsonwebtoken.Claims;
import net.whydah.commands.config.ConstantValue;
import net.whydah.service.oauth2proxyserver.RSAKeyFactory;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import net.whydah.sso.user.types.UserToken;
import org.slf4j.Logger;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by baardl on 15.08.17.
 * Modified by huy on 12.06.20
 * Upgrade to support OpenID Connect
 */
public class AccessTokenMapper {
    private static final Logger log = getLogger(AccessTokenMapper.class);

    //Within the OpenID Connect specification, the standard scopes are defined as openid, profile, email, address, and phone
    //If you use any scope beyond those, you’re beyond the OIDC specification and back into general OAuth and this is where it gets complicated.
    private static final String SCOPE_OPENID = "openid";
    private static final String SCOPE_PROFILE = "profile"; //a lack of support for a profile url (for example: https://fb.com/me) in UserToken, but can possibly define it as a usertoken's role
    private static final String SCOPE_EMAIL = "email";
    private static final String SCOPE_ADDRESS = "address"; //we lack this field in UserToken
    private static final String SCOPE_PHONE = "phone";

    public static Set<String> getWhitelistedRolePatternsForScope(List<String> scopes, Map<String, Set<String>> jwtRolesByScope) {
        Set<String> patternUnion = new LinkedHashSet<>();
        for (String scope : scopes) {
            Set<String> patterns = jwtRolesByScope.get(scope);
            if (patterns != null) {
                patternUnion.addAll(patterns);
            }
        }
        return patternUnion;
    }

    public static boolean isRoleInWhitelistForScope(Set<String> whitelistPatterns, UserApplicationRoleEntry roleEntry) {
        String roleName = roleEntry.getRoleName();
        if (whitelistPatterns.contains(roleName)) {
            return true;
        }
        for (String pattern : whitelistPatterns) {
            if (pattern.endsWith("*")) {
                String prefix = pattern.substring(0, pattern.length() - 1);
                if (roleName.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Map<String, String> getClaimsForUserRoles(List<String> userAuthorizedScope, List<UserApplicationRoleEntry> roleList, Map<String, Set<String>> jwtRolesByScope) {
        Map<String, String> claims = new LinkedHashMap<>();
        Set<String> rolePatterns = getWhitelistedRolePatternsForScope(userAuthorizedScope, jwtRolesByScope);
        for (UserApplicationRoleEntry userApplicationRoleEntry : roleList) {
            if (isRoleInWhitelistForScope(rolePatterns, userApplicationRoleEntry)) {
                claims.put("role_" + userApplicationRoleEntry.getRoleName(), userApplicationRoleEntry.getRoleValue());
            }
        }
        return claims;
    }

    public static String buildToken(UserToken userToken, String clientId, String applicationId, String applicationName, String applicationUrl, String nonce, List<String> userAuthorizedScope, Map<String, Set<String>> jwtRolesByScope) {
        String accessToken = null;
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

            JsonObjectBuilder tokenBuilder = Json.createObjectBuilder()
                    .add("access_token", buildAccessToken(userToken, clientId, applicationId, applicationName, applicationUrl, nonce, userAuthorizedScope, jwtRolesByScope, expiration)) //this client will use this to access other servers' resources
                    .add("token_type", "bearer")
                    .add("nonce", nonce)
                    .add("expires_in", expireSec) //in seconds
                    .add("refresh_token", ClientIDUtil.encrypt(userToken.getUserTokenId() + ":" + String.join(" ", userAuthorizedScope)));

            if (userAuthorizedScope.contains(SCOPE_OPENID)) {
                //OpenID Connect requires "id_token"
                tokenBuilder = tokenBuilder.add("id_token", buildClientToken(userToken, clientId, applicationId, applicationName, applicationUrl, nonce, userAuthorizedScope, jwtRolesByScope, expiration)) //attach granted scopes to JWT
                        .add("nonce", nonce);

            } else {
                //back to general OAuth
                tokenBuilder = buildUserInfoJson(tokenBuilder, userToken, applicationId, userAuthorizedScope);
            }
            accessToken = tokenBuilder.build().toString();
        }
        log.info("token built: {}", accessToken);
        return accessToken;
    }

    public static String buildTokenForClientCredentialGrantType(String clientId, String applicationId, String applicationName, String applicationUrl, String nonce) throws Exception {
        String accessToken = null;
        if (nonce == null) {
            nonce = "";
        }

        Date expiration = new Date(System.currentTimeMillis() + ConstantValue.DF_JWT_LIFESPAN);

        JsonObjectBuilder tokenBuilder = Json.createObjectBuilder()
                .add("access_token", buildAccessTokenForClientCredetntialGrantType(clientId, applicationId, applicationName, applicationUrl, nonce, expiration))//this client will use this to access other servers' resources
                .add("token_type", "bearer")
                .add("nonce", nonce)
                .add("expires_in", ConstantValue.DF_JWT_LIFESPAN / 1000);


        accessToken = tokenBuilder.build().toString();

        log.info("buildTokenForClientCredentialGrantType built: {}", accessToken);
        return accessToken;
    }

    public static JsonObjectBuilder buildUserInfoJson(JsonObjectBuilder tokenBuilder, UserToken userToken, String applicationId, List<String> userAuthorizedScope) {
        if (userAuthorizedScope.contains(SCOPE_EMAIL)) {
            tokenBuilder = tokenBuilder.add(SCOPE_EMAIL, userToken.getEmail());
        }
        if (userAuthorizedScope.contains(SCOPE_PHONE)) {
            tokenBuilder = tokenBuilder.add(SCOPE_PHONE, userToken.getCellPhone());
        }
        tokenBuilder = buildRoles(userToken.getRoleList(), applicationId, userAuthorizedScope, tokenBuilder);
        return tokenBuilder;
    }

    private static String buildClientToken(UserToken userToken, String clientId, String applicationId, String applicationName, String applicationUrl, String nonce, List<String> userAuthorizedScope, Map<String, Set<String>> jwtRolesByScope, Date expiration) {
        if (nonce == null) {
            nonce = "";
        }

        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put(Claims.ID, UUID.randomUUID().toString());
        claims.put(Claims.SUBJECT, userToken.getUserName());
        claims.put(Claims.AUDIENCE, clientId);
        claims.put("app_url", applicationUrl != null ? applicationUrl : applicationName);
        claims.put(Claims.ISSUER, ConstantValue.MYURI);
        claims.put("nonce", nonce);
        claims.put("first_name", userToken.getFirstName());
        claims.put("last_name", userToken.getLastName());
        claims.put("given_name", userToken.getFirstName());
        claims.put("family_name", userToken.getLastName());
        claims.put("customer_ref", userToken.getPersonRef());

        if (userAuthorizedScope.contains(SCOPE_EMAIL)) {
            claims.put(SCOPE_EMAIL, userToken.getEmail());
        }
        if (userAuthorizedScope.contains(SCOPE_PHONE)) {
            claims.put(SCOPE_PHONE, userToken.getCellPhone());
        }
        //for profile, address and other custom scopes we can look into this
        for (UserApplicationRoleEntry role : userToken.getRoleList()) {
            if (role.getApplicationId().equals(applicationId)) {
                if (userAuthorizedScope.contains(role.getRoleName())) {
                    claims.put(role.getRoleName(), role.getRoleValue());
                }
            }
        }
        claims.putAll(getClaimsForUserRoles(userAuthorizedScope, userToken.getRoleList(), jwtRolesByScope));

        return JwtUtils.generateJwtToken(claims, expiration, RSAKeyFactory.getKey().getPrivate());
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
        claims.put(Claims.ISSUER, ConstantValue.MYURI);
        //useful info for back-end services
        claims.put("app_id", appId); //used by other back-end services
        claims.put("app_name", applicationName); //used by other back-end services
        claims.put("customer_ref", usertoken.getPersonRef()); //used by other back-end services
        claims.put("usertoken_id", usertoken.getUserTokenId()); //used by other back-end services
        claims.put("scope", String.join(" ", userAuthorizedScope));  //used for /userinfo endpoint, re-populating user info with this granted scope list
        claims.putAll(getClaimsForUserRoles(userAuthorizedScope, usertoken.getRoleList(), jwtRolesByScope));
        return JwtUtils.generateJwtToken(claims, expiration, RSAKeyFactory.getKey().getPrivate());
    }

    public static String buildAccessTokenForClientCredetntialGrantType(String clientId, String appId, String applicationName, String applicationUrl, String nonce, Date expiration) {
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
        claims.put(Claims.ISSUER, ConstantValue.MYURI);
        //useful info for back-end services
        claims.put("app_id", appId); //used by other back-end services
        claims.put("app_name", applicationName); //used by other back-end services
        return JwtUtils.generateJwtToken(claims, expiration, RSAKeyFactory.getKey().getPrivate());
    }

    protected static JsonObjectBuilder buildRoles(List<UserApplicationRoleEntry> roleList, String applicationId, List<String> userAuthorizedScope, JsonObjectBuilder tokenBuilder) {
        for (UserApplicationRoleEntry role : roleList) {
            if (role.getApplicationId().equals(applicationId)) {
                if (userAuthorizedScope.contains(role.getRoleName())) {
                    tokenBuilder.add(role.getRoleName(), role.getRoleValue());
                }
            }
        }
        return tokenBuilder;
    }
}
