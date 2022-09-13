package net.whydah.service.authorizations;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Repository;

import com.hazelcast.map.IMap;

import net.whydah.util.HazelcastMapHelper;

@Repository
public class SSOUserSessionRepository {
	
	private static boolean byPassRemoval = true;
	
	private IMap<String, AuthenticationSession> userSessions = HazelcastMapHelper.register("SSOAuthSession_Map");

	public void addSession(AuthenticationSession session){
		if (session != null) {
			userSessions.put(session.getId(), session);
		}
	}
	
	public AuthenticationSession getSession(String sessionId) {		
		Iterator<Map.Entry<String, AuthenticationSession>> it = userSessions.entrySet().iterator();
		Date currTime = new Date();
		while (it.hasNext()) {
			Map.Entry<String, AuthenticationSession> entry = it.next();
			long diffInSeconds = TimeUnit.MILLISECONDS.
					toSeconds(currTime.getTime() - entry.getValue().getTimeCreated().getTime());

			if (diffInSeconds > 86400) {
				userSessions.remove(entry.getKey());
			}
		}
		if(!byPassRemoval) {
			return userSessions.remove(sessionId);
		} else {
			return userSessions.get(sessionId);
		}
	}

}
