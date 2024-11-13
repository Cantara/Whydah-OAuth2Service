package net.whydah.service.clients;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * Created by baardl on 11.08.17.
 */
public class Client implements Serializable {
    private final String clientId;
    private String applicationId;
    private String applicationName = "";
    private String applicationUrl = "";
    private String logoUrl = "";
    private String redirectUrl;
    private Map<String, Set<String>> jwtRolesByScope;

    public Client(String clientId, String applicationId, String applicationName, String applicationUrl, String logoUrl, String redirectUrl, Map<String, Set<String>> jwtRolesByScope) {
        this.clientId = clientId;
        this.applicationId = applicationId;
        this.applicationName = applicationName;
        this.applicationUrl = applicationUrl;
        this.logoUrl = logoUrl;
        this.redirectUrl = redirectUrl;
        this.jwtRolesByScope = jwtRolesByScope;
    }

    @Override
    public String toString() {
        return "Client{" +
                "clientId='" + clientId + '\'' +
                ", applicationId='" + applicationId + '\'' +
                ", applicationName='" + applicationName + '\'' +
                ", applicationUrl='" + applicationUrl + '\'' +
                ", logoUrl='" + logoUrl + '\'' +
                ", redirectUrl='" + redirectUrl + '\'' +
                ", jwtRolesByScope='" + jwtRolesByScope + '\'' +
                '}';
    }

    public String getClientId() {
        return clientId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationUrl() {
        return applicationUrl;
    }

    public void setApplicationUrl(String applicationUrl) {
        this.applicationUrl = applicationUrl;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public Map<String, Set<String>> getJwtRolesByScope() {
        return jwtRolesByScope;
    }

    public Client setJwtRolesByScope(Map<String, Set<String>> jwtRolesByScope) {
        this.jwtRolesByScope = jwtRolesByScope;
        return this;
    }
}
