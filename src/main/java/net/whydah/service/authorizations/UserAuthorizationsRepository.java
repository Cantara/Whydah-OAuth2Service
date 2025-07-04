package net.whydah.service.authorizations;

import com.hazelcast.map.IMap;
import jakarta.inject.Singleton;
import net.whydah.util.HazelcastMapHelper;
import org.springframework.stereotype.Repository;

/**
 * Created by baardl on 09.08.17.
 */
@Singleton
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
