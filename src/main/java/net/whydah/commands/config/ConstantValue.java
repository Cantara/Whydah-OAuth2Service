package net.whydah.commands.config;

import net.whydah.util.Configuration;

public class ConstantValue {

    public static final int COMMAND_TIMEOUT = 10000;

    public static final String KEYSECRET = Configuration.getString("oauth2.module.keysecret");
    public static final String MYURI = Configuration.getString("myuri");
    public static final String SSO_URI = Configuration.getString("ssoservice");
    public static final String STS_URI = Configuration.getString("securitytokenservice");
    public static final String LOGOURL = Configuration.getString("logourl");

    public static final String ATOKEN = "AsT5OjbzRn430zqMLgV3Ia"; //for testing   
    //configurations for integration test and providing a dummy token
    public static final String TEST_APPNAME = Configuration.getString("oauth.dummy.applicationname");
    public static final String TEST_APPSECRET = Configuration.getString("oauth.dummy.applicationsecret");
    public static final String TEST_APPID = Configuration.getString("oauth.dummy.applicationid");
    public static final String TEST_USERNAME = Configuration.getString("oauth.dummy.username");
    public static final String TEST_PASSWORD = Configuration.getString("oauth.dummy.password");
    public static final boolean TEST_DUMMY_TOKEN_ENABLED = Configuration.getBoolean("token_dummy_enabled"); 
    
    public static final long DF_JWT_LIFESPAN = 3 * 60 * 60 * 1000; //1 hour;
    public static final boolean TOKEN_CUSTOM_EXPIRY_ENABLED = Configuration.getBoolean("token_custom_expiry");
    public static final long CUSTOM_JWT_LIFESPAN = Long.valueOf(Configuration.getInt("oauth.expiry"));
}
