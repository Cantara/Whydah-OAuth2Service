package net.whydah.service.authorizations;

import java.util.List;

/**
 * Created by baardl on 09.08.17.
 */
public class UserAuthorization {

    private final String code;
    private final List<String> scopes;

    public UserAuthorization(String code, List<String> scopes) {
        if (code == null) {
            throw new IllegalArgumentException("null is not allowed for \"code\".");
        }
        if (scopes == null) {
            throw new IllegalArgumentException("null is not allowed for \"scopes\".");
        }
        this.code = code;
        this.scopes = scopes;
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
}
