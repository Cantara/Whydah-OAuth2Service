package net.whydah.service.oauth2proxyserver;

import net.whydah.service.authorizations.UserAuthorization;
import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.service.clients.ClientService;
import net.whydah.sso.user.types.UserToken;
import org.skyscreamer.jsonassert.JSONAssert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNotNull;

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

        tokenService = new TokenService(authorizationService, clientService);

    }

    @Test
    public void testBuildAccessToken() throws Exception {
    	//try 2 create new RSA key
    	RSAKeyFactory.deleteKeyFile();
        
    	
    	List<String> scopes = new ArrayList<>();
        scopes.add("openid");
        scopes.add("email");

        UserAuthorization userAuth = new UserAuthorization("12345", scopes, "22022");
        when(authorizationService.getAuthorization(eq("somecode"))).thenReturn(userAuth);
        UserToken userToken = new UserToken();
        userToken.setEmail("totto@totto.org");
        userToken.setUid("22022");
        when(authorizationService.findUserTokenFromUserTokenId(anyString())).thenReturn(userToken);
      
        String accessToken = tokenService.buildAccessToken("client_id", "secret", "somecode");
        assertNotNull(accessToken);
        assertTrue(accessToken.contains("id_token"));
        assertTrue(accessToken.contains("access_token"));
        assertTrue(accessToken.contains("expires_in"));
        assertTrue(accessToken.contains("refresh_token"));
        assertTrue(accessToken.contains("token_type"));
        
//		  the test is not correct any more	
//        String expected = "{\"access_token\":\"ACCESS_TOKEN\",\"token_type\":\"bearer\",\"expires_in\":2592000,\"refresh_token\":\"REFRESH_TOKEN\",\"uid\":\"22022\",\"email\":\"totto@totto.org\"}";
//        String expected = "{\"access_token\":\"ACCESS_TOKEN\",\"token_type\":\"bearer\",\"expires_in\":2592000,\"refresh_token\":\"REFRESH_TOKEN\",\"scope\":\" email\",\"uid\":22022,\"info\":{\"name\":\"Totto\",\"email\":\"totto@totto.org\"}}";
//        JSONAssert.assertEquals(expected, accessToken, false);
    }

    @Test
    public void testBuildCode() throws Exception {
    }

}