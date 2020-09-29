package net.whydah.service.clients;

import org.slf4j.Logger;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by baardl on 11.08.17.
 */
@Repository
public class ClientRepository {
    private static final Logger log = getLogger(ClientRepository.class);

    private Map<String, Client> clients = new HashMap<>();

    public void addClient(Client client) {
        if (client != null) {
            clients.put(client.getClientId(), client);
            log.info("Adding client:" + client);
        }
    }

    public Client getClientByClientId(String clientId) {
        Client client = null;
        if (clientId != null && !clientId.isEmpty()) {
            client = clients.get(clientId);
        }
        return client;
    }

    void replaceClients(Map<String, Client> newclients) {
        log.trace("Replacing clients with updated version.");
        if (newclients != null && newclients.size() > 0) {
            this.clients = newclients;
            log.info("Replaced clients");
        }
        log.debug("Replaced {} clients", clients.entrySet().size());
    }

    public boolean clientsEmpty() {
        if (clients == null || clients.size() < 1) {
            return true;
        }
        return false;
    }

    public Collection<Client> allClients() {
        if (clients == null) {
            clients = new HashMap<>();
        }
        return clients.values();
    }
}
