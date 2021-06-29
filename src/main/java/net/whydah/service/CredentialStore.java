package net.whydah.service;

import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.session.WhydahApplicationSession2;
import net.whydah.sso.session.WhydahUserSession2;
import net.whydah.sso.user.types.UserCredential;
import org.constretto.annotation.Configuration;
import org.constretto.annotation.Configure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.inject.Singleton;

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
    private static WhydahUserSession2 adminUserSession = null;


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
        this.uasUri = uasUri;
        this.myApplicationCredential = new ApplicationCredential(applicationid, applicationname, applicationsecret);
        this.adminUserCredential = new UserCredential(adminuserid,adminusersecret);
        this.was = WhydahApplicationSession2.getInstance(stsUri, uasUri, myApplicationCredential);
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
    	} catch(Exception ex) {
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
        if (adminUserSession == null) {
            adminUserSession = getAdminUserSession();
        }
        return adminUserSession.getActiveUserTokenId();
    }
    public WhydahUserSession2 getAdminUserSession() {
        if (adminUserSession == null) {
            adminUserSession =  new WhydahUserSession2(was,adminUserCredential);
        }
        return adminUserSession;
    }

}
