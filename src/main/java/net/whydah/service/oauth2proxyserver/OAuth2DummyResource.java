package net.whydah.service.oauth2proxyserver;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.emory.mathcs.backport.java.util.Arrays;
import net.whydah.commands.config.ConstantValue;
import net.whydah.service.authorizations.UserAuthorization;
import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.service.errorhandling.AppException;
import net.whydah.sso.application.helpers.ApplicationXpathHelper;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.commands.appauth.CommandLogonApplication;
import net.whydah.sso.commands.userauth.CommandLogonUserByUserCredential;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserCredential;
import net.whydah.sso.user.types.UserToken;
import net.whydah.util.ClientIDUtil;

//this is for integration tests where we want to get a dummy JWT 
@Path(OAuth2DummyResource.OAUTH2DUMMY_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class OAuth2DummyResource {

	public static final String OAUTH2DUMMY_PATH = "/token/dummy";
	private static final Logger log = LoggerFactory.getLogger(OAuth2DummyResource.class);
	static String TEMPORARY_APPLICATION_ID = ConstantValue.TEST_APPID;
	static String TEMPORARY_APPLICATION_NAME = ConstantValue.TEST_APPNAME;
	static String TEMPORARY_APPLICATION_SECRET = ConstantValue.TEST_APPSECRET;
	static String TEST_USERNAME = ConstantValue.TEST_USERNAME;
	static String TEST_USERPASSWORD = ConstantValue.TEST_PASSWORD;
    static String clientId = ClientIDUtil.getClientID(TEMPORARY_APPLICATION_ID);
    private final TokenService tokenAuthorizationService;
    private final UserAuthorizationService userAuthorizationService;
    
    @Autowired
    public OAuth2DummyResource(TokenService tokenAuthorizationService, UserAuthorizationService userAuthorizationService) {      
        this.tokenAuthorizationService = tokenAuthorizationService;
        this.userAuthorizationService = userAuthorizationService;
    }
    
    private UserToken getUserToken() {
    	String myAppTokenXml = new CommandLogonApplication(URI.create(ConstantValue.STS_URI), new ApplicationCredential(TEMPORARY_APPLICATION_ID, TEMPORARY_APPLICATION_NAME, TEMPORARY_APPLICATION_SECRET)).execute();
    	String myApplicationTokenID = ApplicationXpathHelper.getAppTokenIdFromAppTokenXml(myAppTokenXml);
    	String userticket = UUID.randomUUID().toString();
    	String userToken = new CommandLogonUserByUserCredential(URI.create(ConstantValue.STS_URI), myApplicationTokenID, myAppTokenXml, new UserCredential(TEST_USERNAME, TEST_USERPASSWORD), userticket).execute();
    	return UserTokenMapper.fromUserTokenXml(userToken);
    }

    
//	@GET
//    public Response getADummyAccessToken() throws Exception {
//		UserToken uToken = getUserToken();
//		List<String> scopes = Arrays.asList(new String[] {"openid", "profile", "email", "phone"});
//		UserAuthorization u = new UserAuthorization(UUID.randomUUID().toString(), scopes , uToken.getUid(), uToken.getUserTokenId());
//		u.setClientId(clientId);
//		//add a new user authorization
//		this.userAuthorizationService.addAuthorization(u);
//		//build a new token based on a user authorization object
//		String accessToken = this.tokenAuthorizationService.buildAccessToken(clientId, "", u.getCode());
//
//		 if (accessToken == null) {
//             log.error("No accessToken provided");
//             return Response.status(Response.Status.FORBIDDEN).build();
//         } else {
//             return Response.ok(accessToken).build();
//         }
//    }
    
    @GET
    public Response getADummyAccessToken() throws Exception, AppException {
    	if(!ConstantValue.TEST_DUMMY_TOKEN_ENABLED) {
    		return Response.status(Response.Status.FORBIDDEN).build();
    	} else {
    		UserToken uToken = getUserToken();
    		List<String> scopes = Arrays.asList(new String[] {"openid", "profile", "email", "phone"});
    		//build a new token based on a user authorization object
    		String accessToken = this.tokenAuthorizationService.buildAccessToken(clientId, uToken.getUserTokenId(), scopes);

    		if (accessToken == null) {
    			log.error("No accessToken provided");
    			return Response.status(Response.Status.FORBIDDEN).build();
    		} else {
    			return Response.ok(accessToken).build();
    		}
    	}
    }

	    
}
