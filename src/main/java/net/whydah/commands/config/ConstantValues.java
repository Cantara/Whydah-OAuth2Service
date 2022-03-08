package net.whydah.commands.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.whydah.util.Configuration;

public class ConstantValues {

	 
   
    public static final int COMMAND_TIMEOUT = 10000;
    public static long  DF_JWT_LIFESPAN = 3 * 60 * 60 * 1000; //1 hour;
    
    public static String KEYSECRET;
    public static String MYURI;
    public static String SSO_URI;
    public static String STS_URI;
    private static String LOGOURL;

    public static String ATOKEN; //for testing   
    //configurations for integration test and providing a dummy token
    public static String TEST_APPNAME;
    public static String TEST_APPSECRET;
    public static String TEST_APPID;
    public static String TEST_USERNAME;
    public static String TEST_PASSWORD;
    public static boolean TEST_DUMMY_TOKEN_ENABLED; 
    
    public static boolean LOGOUT_CONFIRM_ENABLED = false; 
    
    public static boolean CONSENT_SCOPES_ENABLED; 
    
    
    public static String RSA_KEY_ID;
   
	public static String getLogoUrl() {
		String LOGOURL = "/sso/images/site-logo.png";
		try {
			if (ConstantValues.LOGOURL != null && ConstantValues.LOGOURL.length() > 5)
				LOGOURL = ConstantValues.LOGOURL;
		} catch (Exception e) {
			
		}
		return LOGOURL;
	}
	

	static {
		
		KEYSECRET = Configuration.getString("oauth2.module.keysecret");
	    MYURI = Configuration.getString("myuri");
	    SSO_URI = Configuration.getString("ssoservice");
	    STS_URI = Configuration.getString("securitytokenservice");
	    LOGOURL = Configuration.getString("logourl");

	    ATOKEN = "AsT5OjbzRn430zqMLgV3Ia"; //for testing   
	    //configurations for integration test and providing a dummy token
	    TEST_APPNAME = Configuration.getString("oauth.dummy.applicationname");
	    TEST_APPSECRET = Configuration.getString("oauth.dummy.applicationsecret");
	    TEST_APPID = Configuration.getString("oauth.dummy.applicationid");
	    TEST_USERNAME = Configuration.getString("oauth.dummy.username");
	    TEST_PASSWORD = Configuration.getString("oauth.dummy.password");
	    TEST_DUMMY_TOKEN_ENABLED = Configuration.getBoolean("token_dummy_enabled");  
	    CONSENT_SCOPES_ENABLED = Configuration.getBoolean("consent_scopes_enabled");
	    RSA_KEY_ID = Configuration.getString("oauth2.module.rsa_keyid");

	}
    
}
