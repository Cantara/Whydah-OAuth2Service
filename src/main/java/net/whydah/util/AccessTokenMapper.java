package net.whydah.util;

import net.whydah.sso.user.types.UserApplicationRoleEntry;
import net.whydah.sso.user.types.UserToken;
import org.slf4j.Logger;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by baardl on 15.08.17.
 */
public class AccessTokenMapper {
    private static final Logger log = getLogger(AccessTokenMapper.class);
    private static final String SCOPE_UID = "uid";
    private static final String SCOPE_EMAIL = "email";
    private static final String SCOPE_NAME = "name";


    public static String buildToken(UserToken userToken, String applicationId, List<String> userAuthorizedScope) {
        String accessToken = null;
        if (userToken != null) {
            int expireSec = 2592000;
            String refreshToken = "REFRESH_TOKEN";

            JsonObjectBuilder tokenBuilder = Json.createObjectBuilder()
                    .add("access_token", "ACCESS_TOKEN")
                    .add("token_type", "bearer")
                    .add("expires_in", expireSec)
                    .add("refresh_token", refreshToken);
            if (userAuthorizedScope.contains(SCOPE_UID)) {
                tokenBuilder = tokenBuilder.add(SCOPE_UID, userToken.getUid());
            }
            if (userAuthorizedScope.contains(SCOPE_EMAIL)) {
                tokenBuilder = tokenBuilder.add(SCOPE_EMAIL, userToken.getEmail());
            }
            if (userAuthorizedScope.contains(SCOPE_NAME)) {
                tokenBuilder = tokenBuilder.add(SCOPE_NAME, userToken.getUid());
            }
            tokenBuilder = buildRoles(userToken.getRoleList(), applicationId, userAuthorizedScope, tokenBuilder);

            accessToken = tokenBuilder.build().toString();
        }
        return accessToken;
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
