package net.whydah.service.authorizations;

import org.springframework.stereotype.Repository;

import com.hazelcast.map.IMap;

import net.whydah.util.HazelcastMapHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by baardl on 09.08.17.
 */
@Repository
public class UserAuthorizationsRepository {

    private IMap<String, UserAuthorizationSession> userAuthorizations = HazelcastMapHelper.register("UserAuthorizationSession_Map");

    public void addAuthorization(UserAuthorizationSession userAuthorization){
        if (userAuthorization != null && userAuthorization.getCode() != null) {
            userAuthorizations.put(userAuthorization.getCode(), userAuthorization);
        }
    }


    public UserAuthorizationSession getAuthorization(String theUsersAuthorizationCode) {
        return userAuthorizations.remove(theUsersAuthorizationCode); //no need to keep
    }
}
