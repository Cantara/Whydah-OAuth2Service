package net.whydah.service.authorizations;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Repository;

@Repository
public class SSOUserSessionRepository {
	private Map<String, SSOUserSession> userSessions = new HashMap<>();

	public void addSession(SSOUserSession session){
		if (session != null) {
			userSessions.put(session.getId(), session);
		}
	}


	public SSOUserSession getSession(String sessionId) {
		return userSessions.remove(sessionId); //no need to keep
	}

}
