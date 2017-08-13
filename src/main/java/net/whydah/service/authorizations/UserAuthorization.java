package net.whydah.service.authorizations;

import java.util.List;

/**
 * Created by baardl on 09.08.17.
 */
public class UserAuthorization {

    private String userId;
    private final String code;
    private final List<String> scopes;

    /**
     * @deprecated you must have valid Whydah UserId.
     * @param code
     * @param scopes
     */
    public UserAuthorization(String code, List<String> scopes) {
        this(code, scopes, null);

    }

    public UserAuthorization(String code, List<String> scopes, String whydahUserId) {
        if (code == null) {
            throw new IllegalArgumentException("null is not allowed for \"code\".");
        }
        if (scopes == null) {
            throw new IllegalArgumentException("null is not allowed for \"scopes\".");
        }
        this.code = code;
        this.scopes = scopes;
        this.userId = whydahUserId;
    }

    public String getCode() {
        return code;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public boolean isAuthorized(String scope) {
        boolean isAuthorized = false;
        if (scopes != null) {
            isAuthorized = scopes.contains(scope);
        }
        return isAuthorized;
    }

    public String buildScopeString() {
        String scopeStr = "";
        for (String scope : scopes) {
            scopeStr += " " + scope;
        }

        return scopeStr;
    }

    public String getUserId() {
        return userId;
    }

}
