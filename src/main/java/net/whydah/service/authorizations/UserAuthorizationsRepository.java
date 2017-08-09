package net.whydah.service.authorizations;

import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by baardl on 09.08.17.
 */
@Repository
public class UserAuthorizationsRepository {

    private Map<String, UserAuthorization> userAuthorizations = new HashMap<>();

    public void addAuthorization(UserAuthorization userAuthorization){
        if (userAuthorization != null && userAuthorization.getCode() != null) {
            userAuthorizations.put(userAuthorization.getCode(), userAuthorization);
        }
    }


    public UserAuthorization getAuthorization(String theUsersAuthorizationCode) {
        return userAuthorizations.get(theUsersAuthorizationCode);
    }
}
