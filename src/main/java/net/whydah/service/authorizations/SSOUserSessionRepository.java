package net.whydah.service.authorizations;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.hazelcast.map.IMap;

import net.whydah.util.HazelcastMapHelper;

@Repository
public class SSOUserSessionRepository {
	private IMap<String, SSOUserSession> userSessions = HazelcastMapHelper.register("SSOUserSession_Map");

	public void addSession(SSOUserSession session){
		if (session != null) {
			userSessions.put(session.getId(), session);
		}
	}


	public SSOUserSession getSession(String sessionId) {
		return userSessions.remove(sessionId); //no need to keep
	}

}
