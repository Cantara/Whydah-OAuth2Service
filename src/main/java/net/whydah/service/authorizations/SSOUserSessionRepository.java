package net.whydah.service.authorizations;

import com.hazelcast.map.IMap;
import jakarta.inject.Singleton;
import net.whydah.util.HazelcastMapHelper;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
@Repository
public class SSOUserSessionRepository {

    private static boolean byPassRemoval = true;

    private IMap<String, OAuthenticationSession> userSessions = HazelcastMapHelper.register("OAuthenticationSession_Map");

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public SSOUserSessionRepository() {
        executorService.scheduleWithFixedDelay(this::removeExpiredSessions, 30, 30, TimeUnit.SECONDS);
    }

    private void removeExpiredSessions() {
        Date currTime = new Date();
        for (String sessionId : userSessions.localKeySet()) {
            OAuthenticationSession session = userSessions.get(sessionId);
            long diffInSeconds = TimeUnit.MILLISECONDS.
                    toSeconds(currTime.getTime() - session.getTimeCreated().getTime());

            // TODO make the session timeout configurable
            if (diffInSeconds > 86400) {
                // session expired
                userSessions.remove(sessionId);
            }
        }
    }

    public void addSession(OAuthenticationSession session) {
        if (session != null) {
            userSessions.put(session.getId(), session);
        }
    }

    public OAuthenticationSession getSession(String sessionId) {
        if (!byPassRemoval) {
            return userSessions.remove(sessionId);
        } else {
            return userSessions.get(sessionId);
        }
    }

}
