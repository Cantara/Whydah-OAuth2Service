package net.whydah.service;

import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.session.WhydahApplicationSession;
import net.whydah.sso.session.WhydahUserSession;
import net.whydah.sso.user.types.UserCredential;
import org.constretto.annotation.Configuration;
import org.constretto.annotation.Configure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.net.URI;

/**
 * @author <a href="bard.lind@gmail.com">Bard Lind</a>
 */
@Repository
public class CredentialStore {
    private static WhydahApplicationSession was = null;
    private final String stsUri;
    private final ApplicationCredential uasApplicationCredential;
    private final UserCredential adminUserCredential;
    private static WhydahUserSession adminUserSession = null;
    private final URI uasUri;


    @Autowired
    @Configure
    public CredentialStore(@Configuration("securitytokenservice") String stsUri,
                           @Configuration("useradminservice") String uasUri,
                           @Configuration("applicationid") String applicationid,
                           @Configuration("applicationname") String applicationname,
                           @Configuration("applicationsecret") String applicationsecret,
                           @Configuration("adminuserid") String adminuserid,
                           @Configuration("adminusersecret") String adminusersecret) {
        this.stsUri = stsUri;
        this.uasUri = URI.create(uasUri);
        this.uasApplicationCredential = new ApplicationCredential(applicationid, applicationname, applicationsecret);
        this.adminUserCredential = new UserCredential(adminuserid,adminusersecret);
        was = WhydahApplicationSession.getInstance(stsUri, uasUri, uasApplicationCredential.getApplicationID(), uasApplicationCredential.getApplicationName(), uasApplicationCredential.getApplicationSecret());

    }


    public String getUserAdminServiceTokenId() {
        if (was == null) {
            was = WhydahApplicationSession.getInstance(stsUri, uasApplicationCredential.getApplicationID(), uasApplicationCredential.getApplicationName(), uasApplicationCredential.getApplicationSecret());
        }
        return was.getActiveApplicationTokenId();

    }

    public boolean hasWhydahConnection() {
        if (was == null) return false;
        return getWas().checkActiveSession();
    }


    public String hasApplicationToken() {
        try {
            if (getWas() != null) {
                return Boolean.toString(getWas().getActiveApplicationTokenId() != null);
            }
        } catch (Exception e) {
        }
        return Boolean.toString(false);
    }

    public String hasValidApplicationToken() {
        try {
            if (getWas() != null) {
                return Boolean.toString(getWas().checkActiveSession());
            }
        } catch (Exception e) {
        }
        return Boolean.toString(false);
    }

    public String hasApplicationsMetadata() {
        try {
            if (getWas() != null) {
                was.updateApplinks(true);
                return Boolean.toString(getWas().getApplicationList().size() > 2);
            }
        } catch (Exception e) {
        }
        return Boolean.toString(false);
    }


    public WhydahApplicationSession getWas() {
        if (was == null) {
            was = WhydahApplicationSession.getInstance(stsUri, uasApplicationCredential.getApplicationID(), uasApplicationCredential.getApplicationName(), uasApplicationCredential.getApplicationSecret());
            was.updateApplinks(true);
        }
        return was;
    }

    public String getAdminUserTokenId(){
        if (adminUserSession == null) {
            adminUserSession = getAdminUserSession();
        }
        return adminUserSession.getActiveUserTokenId();
    }
    public WhydahUserSession getAdminUserSession() {
        if (adminUserSession == null) {
            adminUserSession =  new WhydahUserSession(getWas(),adminUserCredential);
        }
        return adminUserSession;
    }

    public URI getUAS() {
        return uasUri;
    }
}
