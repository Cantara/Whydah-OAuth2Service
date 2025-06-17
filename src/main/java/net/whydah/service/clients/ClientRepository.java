package net.whydah.service.clients;

import com.hazelcast.map.IMap;
import jakarta.inject.Singleton;
import net.whydah.util.HazelcastMapHelper;
import org.slf4j.Logger;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by baardl on 11.08.17.
 */
@Singleton
@Repository
public class ClientRepository {
    private static final Logger log = getLogger(ClientRepository.class);
    IMap<String, Client> clients = HazelcastMapHelper.register("clients_Map");
    IMap<String, String> codemap = HazelcastMapHelper.register("nonce_Map");
    
    //private static Map<String, Client> clients = new HashMap<>();
    //private static Map<String, String> codemap = new HashMap<>();

    public void addCode(String code, String nonce) {
        codemap.put(code, nonce);
    }

    public String getNonce(String code) {
        return codemap.get(code);
    }

    public void addClient(Client client) {
        if (client != null) {
            clients.put(client.getClientId(), client);
            log.info("Adding client:" + client);
        }
    }

    public Client getClientByClientId(String clientId) {
        Client client = null;
        log.info("Looking for " + clientId + " - RepoSize:" + clients.size() + " - Clients:" + clients.keySet());
        if (clientId != null && !clientId.isEmpty()) {
            client = clients.get(clientId);
        } else {
            log.warn("Attempt to access with unknown clientId");
        }

        return client;
    }

    void replaceClients(Map<String, Client> newclients) {
        log.trace("Replacing clients with updated version.");
        if (newclients != null && newclients.size() > 0) {
            for(Client c : newclients.values()) {
            	clients.put(c.getClientId(), c);
            }
            log.info("Replaced clients, new size:" + clients.size());
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
            return new ArrayList<Client>();
        }
        return clients.values();
    }
}
