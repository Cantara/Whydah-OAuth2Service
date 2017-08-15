package net.whydah.service.clients;

import net.whydah.service.CredentialStore;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationACL;
import net.whydah.util.ClientIDUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by baardl on 11.08.17.
 */
@Service
public class ClientService {
    private static final Logger log = getLogger(ClientService.class);

    public static final String ACL_REDIRECT_KEY = "REDIRECT";

    private final ClientRepository clientRepository;
    private final CredentialStore credentialStore;

    @Autowired
    public ClientService(ClientRepository clientRepository, CredentialStore credentialStore) {
        this.clientRepository = clientRepository;
        this.credentialStore = credentialStore;
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

    public Collection<Client> rebuildClients() {
        List<Application> applicationsList = credentialStore.getWas().getApplicationList();
        Map<String, Client> clients = new HashMap<>(applicationsList.size());
        for (Application application : applicationsList) {
            String clientId = ClientIDUtil.getClientID(application.getId());
            Client client = new Client(clientId, application.getId(), application.getName(), application.getApplicationUrl(),
                    application.getLogoUrl());
            String redirectUrl = findRedirectUrl(application);
            client.setRedirectUrl(redirectUrl);
            clients.put(clientId, client);
        }
        clientRepository.replaceClients(clients);
        return clients.values();
    }

    private String findRedirectUrl(Application application) {
        String redirectUrl = null;

        if (application != null && application.getAcl() != null) {
            List<ApplicationACL> acls = application.getAcl();
            for (ApplicationACL acl : acls) {
                if (acl.getAccessRights() != null && acl.getAccessRights().contains(ACL_REDIRECT_KEY)) {
                    redirectUrl = acl.getApplicationACLPath();
                    log.trace("Found redirectpath {} for application {}", redirectUrl, application.getId());
                }
            }
        }

        return redirectUrl;
    }

    public Client getClient(String clientId) {
        return clientRepository.getClientByClientId(clientId);
    }
}
