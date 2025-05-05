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

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.time.Instant;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;



/**
 * Simple health endpoint for checking the server is running
 *
 * @author <a href="mailto:asbjornwillersrud@gmail.com">Asbjørn Willersrud</a> 30/03/2016.
 */
@Path(HealthResource.HEALTH_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {
    public static final String HEALTH_PATH = "/health";
    private static final Logger log = LoggerFactory.getLogger(HealthResource.class);
    private final CredentialStore credentialStore;
    private final ClientService clientService;
    static String resultJson = "";
    private static String applicationInstanceName = "";



    @Autowired
    public HealthResource(CredentialStore credentialStore, ClientService clientService) {
        this.credentialStore = credentialStore;
        this.clientService = clientService;
        this.applicationInstanceName = Configuration.getString("applicationname");
    }


    @GET
    public Response healthCheck() {
        //log.trace("healthCheck");
        return Response.ok(getHealthTextJson()).build();
    }

    public String getHealthTextJson() {
        return "{\n" +
                "  \"Status\": \"OK\",\n" +
                "  \"Version\": \"" + getVersion() + "\",\n" +
                "  \"now\": \"" + Instant.now().toString() + "\",\n" +
                "  \"IP\": \"" + getMyIPAddresssString() + "\",\n" +
                "  \"DEFCON\": \"" + credentialStore.getWas().getDefcon() + "\",\n" +
                "  \"STS\": \"" + credentialStore.getWas().getSTS() + "\",\n" +
                "  \"UAS\": \"" + credentialStore.getWas().getUAS() + "\",\n" +
                "  \"hasApplicationToken\": \"" + credentialStore.hasApplicationToken() + "\",\n" +
                "  \"hasValidApplicationToken\": \"" + credentialStore.hasValidApplicationToken() + "\",\n" +
                "  \"hasApplicationsMetadata\": \"" + credentialStore.hasApplicationsMetadata() + "\",\n" +
                "  \"ConfiguredApplications\": \"" + credentialStore.getWas().getApplicationList().size() + "\",\n" +

                "  \"running since\": \"" + WhydahUtil.getRunningSince() + "\",\n\n" +

                "  \"clientIDs\": " + getClientIdsJson() + "\n" +
                "}\n";
    }


    private String getVersion() {
        Properties mavenProperties = new Properties();
        String resourcePath = "/META-INF/maven/net.whydah.service/Whydah-OAuth2Service/pom.properties";
        URL mavenVersionResource = this.getClass().getResource(resourcePath);
        if (mavenVersionResource != null) {
            try {
                mavenProperties.load(mavenVersionResource.openStream());
                return mavenProperties.getProperty("version", "missing version info in " + resourcePath) + " [" + applicationInstanceName + " - " + WhydahUtil.getMyIPAddresssesString() + "]";
            } catch (IOException e) {
                log.warn("Problem reading version resource from classpath: ", e);
            }
        }
        return "(DEV VERSION)" + " [" + applicationInstanceName + " - " + WhydahUtil.getMyIPAddresssesString() + "]";
    }

    private synchronized String getClientIdsJson() {
        /*
        if (resultJson != null && resultJson.length() > 10) {
            return resultJson;
        }
        */
        Collection<Client> clients = clientService.allClients();
        if (clients == null || clients.size() < 1) {
            return "[{}]";
        }
        String buildJson = "";
        for (Client client : clients) {
            String logoUrl = client.getLogoUrl();
            if (logoUrl == null) {
                logoUrl = "";
            } else if (logoUrl.length() > 200) {
                logoUrl = "<embedded logo>";
            }

            buildJson = buildJson +
                    "\n     {" +
                    "\n         \"clientId\":          \"" + client.getClientId() + "\"," +
                    "\n         \"applicationName\":   \"" + client.getApplicationName() + "\"," +
                    "\n         \"applicationUrl\":    \"" + client.getApplicationUrl() + "\"," +
                    "\n         \"redirectUrl\":       \"" + client.getRedirectUrl() + "\"," +
                    "\n         \"logoUrl\":           \"" + logoUrl + "\"" +
                    "\n     },";
        }
        if (buildJson.length() < 2) {
            return buildJson;
        }
        buildJson = "\n  [" + buildJson.substring(0, buildJson.length() - 1) + " \n  ]\n";
        resultJson = buildJson;
        return resultJson;
    }



    public static String getMyIPAddresssesString() {
        String ipAdresses = "";

        try {
            ipAdresses = InetAddress.getLocalHost().getHostAddress();
            Enumeration n = NetworkInterface.getNetworkInterfaces();

            while (n.hasMoreElements()) {
                NetworkInterface e = (NetworkInterface) n.nextElement();

                InetAddress addr;
                for (Enumeration a = e.getInetAddresses(); a.hasMoreElements(); ipAdresses = ipAdresses + "  " + addr.getHostAddress()) {
                    addr = (InetAddress) a.nextElement();
                }
            }
        } catch (Exception e) {
            ipAdresses = "Not resolved";
        }

        return ipAdresses;
    }

    public static String getMyIPAddresssString() {
        String fullString = getMyIPAddresssesString();
        return fullString.substring(0, fullString.indexOf(" "));
    }
}