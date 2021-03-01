package net.whydah.service.authorizations;

import java.util.List;

/**
 * Created by baardl on 09.08.17.
 */
public class UserAuthorization {

    private String userId;
    private String clientId;
    private String userTokenId; //Too hard to integrate with userid for now.
    private final String code;
    private final List<String> scopes;
    private String redirectURI;
    private String nonce;

    public UserAuthorization(String code, List<String> scopes, String whydahUserId, String nonce) {
        if (code == null) {
            throw new IllegalArgumentException("null is not allowed for \"code\".");
        }
        if (scopes == null) {
            throw new IllegalArgumentException("null is not allowed for \"scopes\".");
        }
        this.code = code;
        this.scopes = scopes;
        this.userId = whydahUserId;
        this.setNonce(nonce);
    }

    public UserAuthorization(String code, List<String> scopes, String whydahUserId, String redirectURI, String userTokenId, String nonce) {
        this(code,scopes,whydahUserId, nonce);
        this.userTokenId = userTokenId;
        this.redirectURI = redirectURI;
        this.setNonce(nonce);
    }



    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUserTokenId() {
        return userTokenId;
    }

    public void setUserTokenId(String userTokenId) {
        this.userTokenId = userTokenId;
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

	public String getRedirectURI() {
		return redirectURI;
	}

	public void setRedirectURI(String redirectURI) {
		this.redirectURI = redirectURI;
	}

	public String getNonce() {
		return nonce;
	}

	public void setNonce(String nonce) {
		this.nonce = nonce;
	}

}
