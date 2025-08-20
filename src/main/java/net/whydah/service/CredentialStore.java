package net.whydah.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import jakarta.inject.Singleton;
import net.whydah.service.health.HealthResource;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.session.WhydahApplicationSession2;
import net.whydah.sso.session.WhydahUserSession2;
import net.whydah.sso.user.helpers.UserXpathHelper;
import net.whydah.sso.user.types.UserCredential;
import net.whydah.sso.util.WhydahUtil2;
import net.whydah.util.Configuration;

/**
 * @author <a href="bard.lind@gmail.com">Bard Lind</a>
 */
@Singleton
@Repository
public class CredentialStore {
    private final String stsUri;
    private final String uasUri;
    private final ApplicationCredential myApplicationCredential;
    private static WhydahApplicationSession2 was = null;
    private final UserCredential adminUserCredential;
    //private static WhydahUserSession2 adminUserSession = null;
    private static final Logger log = LoggerFactory.getLogger(WhydahUserSession2.class);

    public CredentialStore() {
        // Get properties directly from Configuration class
        this.stsUri = Configuration.getString("securitytokenservice");
        this.uasUri = Configuration.getString("useradminservice");
        String applicationid = Configuration.getString("applicationid");
        String applicationname = Configuration.getString("applicationname");
        String applicationsecret = Configuration.getString("applicationsecret");
        String adminuserid = Configuration.getString("adminuserid");
        String adminusersecret = Configuration.getString("adminusersecret");

        // Log the values to help diagnose issues
        log.info("Initializing CredentialStore with: applicationid={}, applicationname={}",
                applicationid, applicationname);

        this.myApplicationCredential = new ApplicationCredential(applicationid, applicationname, applicationsecret);
        this.adminUserCredential = new UserCredential(adminuserid, adminusersecret);
        this.was = WhydahApplicationSession2.getInstance(stsUri, uasUri, myApplicationCredential);
        HealthResource.setCredentialStore(this);
    }


    public String getActiveApplicationTokenId() {

        if (hasWhydahConnection()){
            return was.getActiveApplicationTokenId();
        }
        return null;
    }

    public boolean hasWhydahConnection() {

        try {
            return getWas().checkActiveSession();
        } catch (Exception ex) {
            return false;
        }
    }


    public String hasApplicationToken() {
        try {
            if (hasWhydahConnection()) {
                return Boolean.toString(getWas().getActiveApplicationTokenId() != null);
            }
        } catch (Exception e) {
        }
        return Boolean.toString(false);
    }

    public String hasValidApplicationToken() {
        try {
            if (hasWhydahConnection()) {
                return Boolean.toString(getWas().checkActiveSession());
            }
        } catch (Exception e) {
        }
        return Boolean.toString(false);
    }

    public String hasApplicationsMetadata() {
        try {
            if (hasWhydahConnection()) {
                was.updateApplinks(true);
                return Boolean.toString(getWas().getApplicationList().size() > 2);
            }
        } catch (Exception e) {
        }
        return Boolean.toString(false);
    }


    public WhydahApplicationSession2 getWas() {
        return was;
    }

    public String getAdminUserTokenId(){
//        if (adminUserSession == null) {
//            adminUserSession = getAdminUserSession();
//        }
//        return adminUserSession.getActiveUserTokenId();
        log.info("Logon useradmin token");
        String userTokenXML = WhydahUtil2.logOnUser(was, this.adminUserCredential);
        if (userTokenXML == null || userTokenXML.length() < 4) {
            log.error("Error, unable to initialize new user session");
        } else {
            log.info("Initializing user session successfull.  userTokenXml:" + userTokenXML);
            return UserXpathHelper.getUserTokenId(userTokenXML);
        }
        return null;

    }
//    public WhydahUserSession2 getAdminUserSession() {
//        if (adminUserSession == null) {
//            adminUserSession =  new WhydahUserSession2(was,adminUserCredential);
//        }
//        return adminUserSession;
//    }


}