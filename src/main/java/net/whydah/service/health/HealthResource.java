package net.whydah.service.health;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.whydah.service.CredentialStore;
import net.whydah.service.clients.Client;
import net.whydah.service.clients.ClientService;
import net.whydah.sso.util.WhydahUtil;
import net.whydah.util.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.time.Instant;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;

@Path(HealthResource.HEALTH_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {
    public static final String HEALTH_PATH = "/health";
    private static final Logger log = LoggerFactory.getLogger(HealthResource.class);

    @Autowired
    private CredentialStore credentialStore;

    @Autowired
    private ClientService clientService;
    static String resultJson = "";
    private static String applicationInstanceName = "";

    public void setCredentialStore(CredentialStore credentialStore) {
        this.credentialStore = credentialStore;
    }

    public void setClientService(ClientService clientService) {
        this.clientService = clientService;
    }

    @Autowired
    public HealthResource(CredentialStore credentialStore, ClientService clientService) {
        this();  // Call the no-arg constructor to set applicationInstanceName
        this.credentialStore = credentialStore;
        this.clientService = clientService;
    }

    // @Autowired
    public HealthResource() {
        try {
            this.applicationInstanceName = Configuration.getString("applicationname");
        } catch (Exception e) {
            this.applicationInstanceName = "Whydah-OAuth2Service";
            log.warn("Unable to get applicationname from properties, using default", e);
        }
    }

    @GET
    public Response healthCheck() {
        log.trace("healthCheck called");
        return Response.ok(getHealthTextJson()).build();
    }

    public String getHealthTextJson() {
        // Make the health check resilient to failures
        String defcon = "UNAVAILABLE";
        String sts = "UNAVAILABLE";
        String uas = "UNAVAILABLE";
        String hasAppToken = "false";
        String hasValidAppToken = "false";
        String hasAppsMetadata = "false";
        String configuredApps = "0";

        try {
            if (credentialStore != null) {
                try {
                    if (credentialStore.getWas() != null) {
                        try {
                            defcon = String.valueOf(credentialStore.getWas().getDefcon());
                        } catch (Exception e) {
                            log.debug("Error getting DEFCON", e);
                        }

                        try {
                            sts = credentialStore.getWas().getSTS();
                        } catch (Exception e) {
                            log.debug("Error getting STS", e);
                        }

                        try {
                            uas = credentialStore.getWas().getUAS();
                        } catch (Exception e) {
                            log.debug("Error getting UAS", e);
                        }

                        try {
                            hasAppToken = credentialStore.hasApplicationToken();
                        } catch (Exception e) {
                            log.debug("Error checking application token", e);
                        }

                        try {
                            hasValidAppToken = credentialStore.hasValidApplicationToken();
                        } catch (Exception e) {
                            log.debug("Error checking valid application token", e);
                        }

                        try {
                            hasAppsMetadata = credentialStore.hasApplicationsMetadata();
                        } catch (Exception e) {
                            log.debug("Error checking applications metadata", e);
                        }

                        try {
                            configuredApps = String.valueOf(credentialStore.getWas().getApplicationList().size());
                        } catch (Exception e) {
                            log.debug("Error getting application list size", e);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Error accessing WAS from credential store", e);
                }
            }
        } catch (Exception e) {
            log.warn("Error getting credential store information", e);
        }

        return "{\n" +
                "  \"Status\": \"OK\",\n" +
                "  \"Version\": \"" + getVersion() + "\",\n" +
                "  \"now\": \"" + Instant.now().toString() + "\",\n" +
                "  \"IP\": \"" + getMyIPAddresssString() + "\",\n" +
                "  \"DEFCON\": \"" + defcon + "\",\n" +
                "  \"STS\": \"" + sts + "\",\n" +
                "  \"UAS\": \"" + uas + "\",\n" +
                "  \"hasApplicationToken\": \"" + hasAppToken + "\",\n" +
                "  \"hasValidApplicationToken\": \"" + hasValidAppToken + "\",\n" +
                "  \"hasApplicationsMetadata\": \"" + hasAppsMetadata + "\",\n" +
                "  \"ConfiguredApplications\": \"" + configuredApps + "\",\n" +
                "  \"running since\": \"" + WhydahUtil.getRunningSince() + "\",\n\n" +
                "  \"clientIDs\": " + getClientIdsJson() + "\n" +
                "}\n";
    }

    private String getVersion() {
        try {
            Properties mavenProperties = new Properties();
            String resourcePath = "/META-INF/maven/net.whydah.service/Whydah-OAuth2Service/pom.properties";
            URL mavenVersionResource = this.getClass().getResource(resourcePath);
            if (mavenVersionResource != null) {
                mavenProperties.load(mavenVersionResource.openStream());
                return mavenProperties.getProperty("version", "missing version info in " + resourcePath) +
                        " [" + applicationInstanceName + " - " + WhydahUtil.getMyIPAddresssesString() + "]";
            }
        } catch (Exception e) {
            log.warn("Problem reading version resource from classpath: ", e);
        }
        return "(DEV VERSION)" + " [" + applicationInstanceName + " - " + getMyIPAddresssString() + "]";
    }

    private synchronized String getClientIdsJson() {
        try {
            Collection<Client> clients = null;
            try {
                if (clientService != null) {
                    clients = clientService.allClients();
                }
            } catch (Exception e) {
                log.debug("Error getting clients from clientService", e);
            }

            if (clients == null || clients.isEmpty()) {
                return "[{}]";
            }

            StringBuilder buildJson = new StringBuilder();
            for (Client client : clients) {
                try {
                    String logoUrl = client.getLogoUrl();
                    if (logoUrl == null) {
                        logoUrl = "";
                    } else if (logoUrl.length() > 200) {
                        logoUrl = "<embedded logo>";
                    }

                    buildJson.append("\n     {")
                            .append("\n         \"clientId\":          \"").append(client.getClientId()).append("\",")
                            .append("\n         \"applicationName\":   \"").append(client.getApplicationName()).append("\",")
                            .append("\n         \"applicationUrl\":    \"").append(client.getApplicationUrl()).append("\",")
                            .append("\n         \"redirectUrl\":       \"").append(client.getRedirectUrl()).append("\",")
                            .append("\n         \"logoUrl\":           \"").append(logoUrl).append("\"")
                            .append("\n     },");
                } catch (Exception e) {
                    log.debug("Error processing client information", e);
                }
            }

            if (buildJson.length() < 2) {
                return "[{}]";
            }
            return "\n  [" + buildJson.substring(0, buildJson.length() - 1) + " \n  ]\n";
        } catch (Exception e) {
            log.warn("Error getting client IDs", e);
            return "[{\"error\": \"Error retrieving client information\"}]";
        }
    }

    public static String getMyIPAddresssesString() {
        String ipAdresses = "";
        try {
            ipAdresses = InetAddress.getLocalHost().getHostAddress();
            Enumeration n = NetworkInterface.getNetworkInterfaces();
            while (n.hasMoreElements()) {
                NetworkInterface e = (NetworkInterface) n.nextElement();
                InetAddress addr;
                for (Enumeration a = e.getInetAddresses(); a.hasMoreElements();
                     ipAdresses = ipAdresses + "  " + addr.getHostAddress()) {
                    addr = (InetAddress) a.nextElement();
                }
            }
        } catch (Exception e) {
            ipAdresses = "Not resolved";
        }
        return ipAdresses;
    }

    public static String getMyIPAddresssString() {
        try {
            String fullString = getMyIPAddresssesString();
            return fullString.substring(0, fullString.indexOf(" "));
        } catch (Exception e) {
            return "IP not available";
        }
    }
}