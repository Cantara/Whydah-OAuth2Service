package net.whydah.service.oauth2proxyserver;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.service.authorizations.UserAuthorizationSession;
import net.whydah.service.clients.Client;
import net.whydah.service.clients.ClientService;
import net.whydah.service.errorhandling.AppException;
import net.whydah.sso.user.types.UserToken;

/**
 * Created by baardl on 14.08.17.
 */
public class TokenServiceTest {
    private TokenService tokenService;
    private UserAuthorizationService authorizationService;
    private ClientService clientService;

    @Before
    public void setUp() throws Exception {
        authorizationService = mock(UserAuthorizationService.class);
        clientService = mock(ClientService.class);
        tokenService = new TokenService(authorizationService, clientService);

    }

    @Ignore
    @Test
    public void testBuildAccessToken() throws Exception, AppException {
    	//try 2 create new RSA key
    	//SAKeyFactory.deleteKeyFile();
        
    	
    	List<String> scopes = new ArrayList<>();
        scopes.add("openid");
        scopes.add("email");

        UserAuthorizationSession userAuth = new UserAuthorizationSession("12345", scopes, "22022","");
        when(authorizationService.getAuthorization(eq("somecode"))).thenReturn(userAuth);
        UserToken userToken = new UserToken();
        userToken.setEmail("totto@totto.org");
        userToken.setUid("22022");
        userAuth.setUserTokenId(userToken.getUserTokenId());
        when(authorizationService.findUserTokenFromUserTokenId(anyString())).thenReturn(userToken);
        when(clientService.getClient(anyString())).thenReturn(new Client("client_id", "101", "ASC Resource", "http://oauh2test.uk", null, null, Collections.emptyMap()));
      
        String accessToken = tokenService.buildAccessToken("client_id", "somecode", "random nonce", userAuth);
        assertNotNull(accessToken);
        assertTrue(accessToken.contains("id_token"));
        assertTrue(accessToken.contains("access_token"));
        assertTrue(accessToken.contains("expires_in"));
        assertTrue(accessToken.contains("nonce"));
        assertTrue(accessToken.contains("refresh_token"));
        assertTrue(accessToken.contains("token_type"));
        
//		  the test is not correct any more	
//        String expected = "{\"access_token\":\"ACCESS_TOKEN\",\"token_type\":\"bearer\",\"expires_in\":2592000,\"refresh_token\":\"REFRESH_TOKEN\",\"uid\":\"22022\",\"email\":\"totto@totto.org\"}";
//        String expected = "{\"access_token\":\"ACCESS_TOKEN\",\"token_type\":\"bearer\",\"expires_in\":2592000,\"refresh_token\":\"REFRESH_TOKEN\",\"scope\":\" email\",\"uid\":22022,\"info\":{\"name\":\"Totto\",\"email\":\"totto@totto.org\"}}";
//        JSONAssert.assertEquals(expected, accessToken, false);
    }

//    @Test
//    public void testBuildCode() throws Exception {
//    }

}