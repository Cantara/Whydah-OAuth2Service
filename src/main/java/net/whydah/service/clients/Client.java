package net.whydah.service.clients;

/**
 * Created by baardl on 11.08.17.
 */
public class Client {
    private final String clientId;
    private String applicationId;
    private String applicationName = "";
    private String applicationUrl = "";
    private String logoUrl = "";
    private String redirectUrl;

    public Client(String clientId) {
        this.clientId = clientId;
    }

    public Client(String clientId, String applicationId) {
        this(clientId);
        this.applicationId = applicationId;
    }

    public Client(String clientId, String applicationId, String applicationName, String applicationUrl, String logoUrl) {
       this(clientId, applicationId);
       this.applicationName = applicationName;
       this.applicationUrl = applicationUrl;
       this.logoUrl = logoUrl;
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


}
