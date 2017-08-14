package net.whydah.service.health;

import net.whydah.service.CredentialStore;
import net.whydah.sso.application.types.Application;
import net.whydah.util.ClientIdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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


    @Autowired
    public HealthResource(CredentialStore credentialStore) {
        this.credentialStore = credentialStore;
    }


    @GET
    public Response healthCheck() {
        log.trace("healthCheck");
        return Response.ok(getHealthTextJson()).build();
    }

    public String getHealthTextJson() {
        return "{\n" +
                "  \"Status\": \"OK\",\n" +
                "  \"Version\": \"" + getVersion() + "\",\n" +
                "  \"DEFCON\": \"" + "DEFCON5" + "\",\n" +
                "  \"hasApplicationToken\": \"" + credentialStore.hasApplicationToken() + "\",\n" +
                "  \"hasValidApplicationToken\": \"" + credentialStore.hasValidApplicationToken() + "\",\n" +
                "  \"hasApplicationsMetadata\": \"" + credentialStore.hasApplicationsMetadata() + "\",\n" +

                "  \"now\": \"" + Instant.now()+ "\",\n" +
                "  \"running since\": \"" + getRunningSince() + "\",\n\n" +

                "  \"clientIDs\": " + getClientIdsJson() + "\n" +
                "}\n";
    }

    private String getRunningSince() {
        long uptimeInMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        return Instant.now().minus(uptimeInMillis, ChronoUnit.MILLIS).toString();
    }

    private String getVersion() {
        Properties mavenProperties = new Properties();
        String resourcePath = "/META-INF/maven/net.whydah.service/Whydah-OAuth2Service/pom.properties";
        URL mavenVersionResource = this.getClass().getResource(resourcePath);
        if (mavenVersionResource != null) {
            try {
                mavenProperties.load(mavenVersionResource.openStream());
                return mavenProperties.getProperty("version", "missing version info in " + resourcePath);
            } catch (IOException e) {
                log.warn("Problem reading version resource from classpath: ", e);
            }
        }
        return "(DEV VERSION)";
    }

    private String getClientIdsJson() {
        String resultJson = "";
        List<Application> applicationsList = credentialStore.getWas().getApplicationList();
        for (Application application : applicationsList) {
            resultJson = resultJson +
                    "\n     {" +
                    "\n         \"clientId\":       \"" + ClientIdUtil.getClientID(application.getId()) + "\"," +
                    "\n         \"applicationName\":\"" + application.getName() + "\"" +
                    "\n     },";
        }
        if (resultJson.length() < 2) {
            return "[]";
        }
        resultJson = "[\n" + resultJson.substring(0, resultJson.length() - 1) + " \n]\n";
        return resultJson;
    }


}