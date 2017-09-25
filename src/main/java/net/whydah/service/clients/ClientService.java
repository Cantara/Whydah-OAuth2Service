package net.whydah.service.clients;

import net.whydah.service.CredentialStore;
import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationACL;
import net.whydah.sso.commands.adminapi.application.CommandGetApplication;
import net.whydah.sso.session.WhydahApplicationSession;
import net.whydah.util.ClientIDUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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
@Service
public class ClientService {
    private static final Logger log = getLogger(ClientService.class);


    private final ClientRepository clientRepository;
    private final CredentialStore credentialStore;
    private static boolean isRunning = false;
    private static ScheduledExecutorService scheduledThreadPool;

    private Instant lastUpdated = null;

    @Autowired
    public ClientService(ClientRepository clientRepository, CredentialStore credentialStore) {
        this.clientRepository = clientRepository;
        this.credentialStore = credentialStore;
        startProcessWorker();
    }

    public boolean isClientValid(String clientId) {
        boolean isValid = false;
        Client client = clientRepository.getClientByClientId(clientId);
        if (client != null) {
            isValid = true;
        } else if (clientId != null && clientId.equals("CLIENT_ID")) {
            //FIXME remove when test no longer need this
            isValid = true;
        }
        return isValid;
    }

    /**
     * Enable syncronization to prevent race conditions.
     * @return List of clients.
     */
    private synchronized Collection<Client> rebuildClients() {
        List<Application> applicationsList = credentialStore.getWas().getApplicationList();
        Map<String, Client> clients = new HashMap<>(applicationsList.size());
        for (Application application : applicationsList) {
            if (application.getTags().contains("HIDDEN")) {
                log.debug("Filtering out Application {}", application);
            } else {
                Client client = buildClient(application);
                String clientId = client.getClientId();
                clients.put(clientId, client);
            }
        }
        clientRepository.replaceClients(clients);
        log.info("Updated {} clients.", clients.size());
        lastUpdated = Instant.now();
        return clients.values();
    }

    private Client buildClient(Application application) {
        Client client = null;
        if (application != null) {
            String clientId = ClientIDUtil.getClientID(application.getId());
            client = new Client(clientId, application.getId(), application.getName(), application.getApplicationUrl(),
                    application.getLogoUrl());
            String redirectUrl = findRedirectUrl(application);
            client.setRedirectUrl(redirectUrl);
        }
        return client;
    }

    public Collection<Client> allClients() {
//        if (updateOutdatedCache()) {
//            rebuildClients();
//        }
        return clientRepository.allClients();

    }


    private String findRedirectUrl(Application application) {
        String redirectUrl = null;

        if (application != null && application.getAcl() != null) {
            List<ApplicationACL> acls = application.getAcl();
            for (ApplicationACL acl : acls) {
                if (acl.getAccessRights() != null && acl.getAccessRights().contains(ApplicationACL.OAUTH2_REDIRECT)) {
                    redirectUrl = acl.getApplicationACLPath();
                    log.trace("Found redirectpath {} for application {}", redirectUrl, application.getId());
                }
            }
        }

        return redirectUrl;
    }

    public Client getClient(String clientId) {

//        if (updateOutdatedCache()) {
//            rebuildClients();
//        }
        Client client = clientRepository.getClientByClientId(clientId);
        if (client == null && throtleOk()) {
            String applicationId = ClientIDUtil.getApplicationId(clientId);
            Application application = fetchApplication(applicationId);
            if (application.getTags().contains("HIDDEN")) {
                log.debug("Filtering out Application {}", application);
            } else {
                // Not filtered - lets map and add the application as a OAuth2 client
                client = buildClient(application);
                if (client != null) {
                    clientRepository.addClient(client);
                }
            }

        }
        return client;
    }

    private Application fetchApplication(String applicationId) {
        Application application = null;
        WhydahApplicationSession was = credentialStore.getWas();
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

    private boolean throtleOk() {
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
            /*
            //Fetch first time.
            if (updateOutdatedCache()) {
                do {
                    startClientRepoUpdater();
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        log.trace("Sleep interupted.");
                    }

                } while (updateOutdatedCache());
            }
            */
            scheduledThreadPool = Executors.newScheduledThreadPool(1);
            //Schedule to Update Cache every 5 minutes.
            log.debug("startProcessWorker - Current Time = " + new Date());
            try {
                scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {
                    public void run() {
                        startClientRepoUpdater();
                    }
                }, 0, 300, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("Error or interrupted trying to refresh client list.", e);
                isRunning = false;
            }

        }
    }


    public void startClientRepoUpdater() {
        long threadId = Thread.currentThread().getId();
        log.info("startClientRepoUpdater accessed, thread: " + threadId);
        rebuildClients();
        isRunning = true;

    }

}
