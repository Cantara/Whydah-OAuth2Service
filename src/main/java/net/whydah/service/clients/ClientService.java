package net.whydah.service.clients;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.whydah.service.CredentialStore;
import net.whydah.service.health.HealthResource;
import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.mappers.ApplicationTagMapper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationACL;
import net.whydah.sso.application.types.Tag;
import net.whydah.sso.commands.adminapi.application.CommandGetApplication;
import net.whydah.sso.session.WhydahApplicationSession2;
import net.whydah.util.ClientIDUtil;
import org.glassfish.hk2.api.Immediate;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by baardl on 11.08.17.
 */
@Singleton
@Service
@Immediate
public class ClientService {
    private static final Logger log = getLogger(ClientService.class);


    private final ClientRepository clientRepository;
    private final CredentialStore credentialStore;
    private static boolean isRunning = false;
    private static ScheduledExecutorService scheduledThreadPool;

    private Instant lastUpdated = null;

    @Inject
    public ClientService(ClientRepository clientRepository, CredentialStore credentialStore) {
        this.clientRepository = clientRepository;
        this.credentialStore = credentialStore;
        startProcessWorker();
        HealthResource.setClientService(this);
    }

    public boolean isClientValid(String clientId) {
        boolean isValid = false;
        log.info("Looking for clientid:" + clientId);
        try {
            Client client = clientRepository.getClientByClientId(clientId);
            log.info("Found client:" + client);
            if (client != null) {
                isValid = true;
            } else if (clientId != null && clientId.equals("CLIENT_ID")) {
                //FIXME remove when test no longer need this
                log.info("checking clientId.equals(\"CLIENT_ID\"):" + clientId.equals("CLIENT_ID"));
                isValid = true;
            }
        } catch (Exception e) {
            log.error("Unhandled exception in trying to determine valid client");
        }
        log.info("Returning:" + isValid);
        return isValid;
    }

    /**
     * Enable syncronization to prevent race conditions.
     * @return List of clients.
     */
    private synchronized Collection<Client> rebuildClients() {
        log.trace("rebuildClients start");
        List<Application> applicationsList = credentialStore.getWas().getApplicationList();
        if (applicationsList.size() < 1) {
            log.warn("Unable to add clients, as we got no applications from Whydah");
        }
        Map<String, Client> clients = new HashMap<>(applicationsList.size());
        for (Application application : applicationsList) {
            if (application.getTags().contains("HIDDEN")) {
                log.debug("Filtering out Application {}", application);
            } else {
                Client client = buildClient(application);
                String clientId = client.getClientId();
                log.debug("Added Application {} as Client {}", application, client);
                clients.put(clientId, client);
                clientRepository.addClient(client);
            }
        }
        log.info("Updating {} clients.", clients.size());
        clientRepository.replaceClients(clients);
        log.info("Updated {} clients.", clients.size());
        lastUpdated = Instant.now();
        return clients.values();
    }

    private Client buildClient(Application application) {
        log.debug("Building client for application:" + application.getName() + " id:" + application.getId());
        Client client = null;
        if (application != null) {
            String clientId = ClientIDUtil.getClientID(application.getId());
            String redirectUrl = findRedirectUrl(application);
            Map<String, Set<String>> jwtRolesByScope = new LinkedHashMap<>();
            for (Tag tag : ApplicationTagMapper.getTagList(application.getTags())) {
                log.debug("  looping through the application tags - tag:" + tag.getName() + " -value:" + tag.getValue());
                String tagname = tag.getName().toLowerCase(); // prefix and scope considered in lower-case only
                if (tagname.startsWith("jwtroles-")) {
                    String scope = tagname.substring("jwtroles-".length());
                    String[] roles = tag.getValue().split(";");
                    log.debug(" found roles from jwtroles tag:" + roles);
                    jwtRolesByScope.put(scope, new LinkedHashSet<>(Arrays.asList(roles)));
                    break;
                }
            }
            client = new Client(clientId, application.getId(), application.getName(), application.getApplicationUrl(),
                    application.getLogoUrl(), redirectUrl, jwtRolesByScope);
            client.setRedirectUrl(redirectUrl);
            log.info("buildClient: {}", client);
            log.info("buildClient: redirectUrl {}", client.getRedirectUrl());
        } else {
            log.warn("Trying to build client from application=null");
        }
        return client;
    }

    public void addCode(String code, String nonce) {
        clientRepository.addCode(code, nonce);
    }

    public String getNonce(String code) {
        return clientRepository.getNonce(code);
    }

    public Collection<Client> allClients() {
        //  if (updateOutdatedCache()) {
        //      rebuildClients();
        //  }
        return clientRepository.allClients();

    }


    private String findRedirectUrl(Application application) {
        String redirectUrl = application.getApplicationUrl();
        log.info("findRedirectUrl - Found getApplicationUrl {} for application {}", redirectUrl, application.getId());

        if (application != null && application.getAcl() != null) {
            List<ApplicationACL> acls = application.getAcl();
            try {
                for (ApplicationACL acl : acls) {
                    try {
                        if (acl.getAccessRights() != null && acl.getAccessRights().contains(ApplicationACL.OAUTH2_REDIRECT)) {
                            String returnedPath = acl.getApplicationACLPath();
                            log.info("findRedirectUrl - Found redirectpath {} for application {}", redirectUrl, application.getId());
                            redirectUrl = returnedPath;
                            /*
                            if (isValidURL(returnedPath)) {
                                redirectUrl = returnedPath;
                            } else {
                                log.error("findRedirectUrl - Found INVALID redirectpath {} for application {}", redirectUrl, application.getId());

                            }*/
                        }
                    } catch (Exception e) {
                        log.error("Unable to map ApplicationACL to oauth credentials", e);
                    }
                }
            } catch (Exception e) {
                log.error("Unable to map application {} to oauth credentials", application.getId(), e);
            }
        }

        log.info("Returning redirectpath {} for application {}", redirectUrl, application.getId());
        return redirectUrl;
    }

    public static boolean isValidURL(String urlString) {
        try {
            URI uri = URI.create(urlString);  // Use URI.create() instead
            // Validate that it's a proper HTTP/HTTPS URL
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return false;
            }
            // Convert to URL to ensure it's valid
            uri.toURL();  // This validates the URI can be converted to URL
            return true;
        } catch (Exception exception) {
            log.warn("isValidURL - Unable to convert redirectURL to URI - parsing: \"" + urlString + "\"");
            return false;
        }
    }

    public Client getClient(String clientId) {

        Client client = clientRepository.getClientByClientId(clientId);
        if (client == null && trottleOk()) {
            String applicationId = ClientIDUtil.getApplicationId(clientId);
            Application application = fetchApplication(applicationId);
            if (application.getTags().contains("HIDDEN")) {
                log.debug("Filtering out ApplicationID {}", application.getId());
            } else {
                // Not filtered - lets map and add the application as a OAuth2 client
                client = buildClient(application);
                if (client != null) {
                    log.debug("Added ApplicationID {} as Client {}", applicationId, client);
                    clientRepository.addClient(client);
                }
            }

        }
        return client;
    }

    public Application getApplicationByClientId(String clientId) {
    	 String applicationId = ClientIDUtil.getApplicationId(clientId);
         Application application = fetchApplication(applicationId);
         return application;
    }

    private Application fetchApplication(String applicationId) {
        Application application = null;
        WhydahApplicationSession2 was = credentialStore.getWas();
        String applicationTokenId = was.getActiveApplicationTokenId();
        String uas = was.getUAS();
        URI userAdminServiceUri = URI.create(uas);
        String adminUserTokenId = credentialStore.getAdminUserTokenId();
        if (adminUserTokenId == null) {
            log.warn("User admin is not logged in.");
        } else {
            String applicationJson = new CommandGetApplication(userAdminServiceUri, applicationTokenId, adminUserTokenId, applicationId).execute();
            log.trace("Found application {} from id {}", applicationJson, applicationId);
            if (applicationJson != null) {
                application = ApplicationMapper.fromJson(applicationJson);
            }
        }
        return application;
    }

    private boolean trottleOk() {
        return true;
    }

    public boolean updateOutdatedCache() {
        boolean updateIsNeeded = false;
        if (lastUpdated == null || clientRepository.clientsEmpty()  ) {
            updateIsNeeded = true;
        } else {
            Instant now = Instant.now();
            Instant fiveMinutesAgo = now.minus(5, ChronoUnit.MINUTES);
            if (lastUpdated.isBefore(fiveMinutesAgo)) {
                updateIsNeeded = true;
            }
        }
        return updateIsNeeded;
    }


    public void startProcessWorker() {
        if (!isRunning) {

            scheduledThreadPool = Executors.newScheduledThreadPool(1);
            //Schedule to Update Cache every 5 minutes.
            log.debug("startProcessWorker - Current Time = " + new Date());
            try {
                scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {
                    public void run() {
                        startClientRepoUpdater();
                    }
                }, 10, 90, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("Error or interrupted trying to refresh client list.", e);
                isRunning = false;
            }

        }
    }


    public void startClientRepoUpdater() {
        long threadId = Thread.currentThread().threadId();
        log.info("startClientRepoUpdater accessed, thread: " + threadId);
        rebuildClients();
        isRunning = true;

    }

    public String getRedirectURI(String client_id, String redirect_url) {
		if (redirect_url == null || redirect_url.isEmpty()) {

			Client client = getClient(client_id);
			if (client != null) {
				log.info("Resolving redirect_uri from clientService.getClient:{}", client);
				redirect_url = client.getRedirectUrl(); //clientService."http://localhost:8888/oauth/generic/callback";
				log.info("Resolving redirect_uri from clientService.getClient.getRedirectUrl(), found:{}", redirect_url);
			}
		}
		if (redirect_url == null || redirect_url.isEmpty()) {

			Client client = getClient(client_id);
			if (client != null) {
				redirect_url = client.getApplicationUrl();
				log.info("Resolving redirect_uri from clientService.getClient.getApplicationUrl(), found:{}", redirect_url);
			}
		}
		return redirect_url;
	}


}
