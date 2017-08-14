package net.whydah.service.oauth2proxyserver;

import net.whydah.service.authorizations.UserAuthorization;
import net.whydah.service.authorizations.UserAuthorizationService;
import org.skyscreamer.jsonassert.JSONAssert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

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

    @BeforeMethod
    public void setUp() throws Exception {
        authorizationService = mock(UserAuthorizationService.class);

        tokenService = new TokenService(authorizationService);

    }

    @Test
    public void testBuildAccessToken() throws Exception {
        List<String> scopes = new ArrayList<>();
        scopes.add("email");
        UserAuthorization userAuth = new UserAuthorization("12345", scopes, "userid");
        when(authorizationService.getAuthorization(eq("somecode"))).thenReturn(userAuth);
        String accessToken = tokenService.buildAccessToken("client_id", "secet", "somecode");
        assertNotNull(accessToken);
        String expected = "{\"access_token\":\"ACCESS_TOKEN\",\"token_type\":\"bearer\",\"expires_in\":2592000,\"refresh_token\":\"REFRESH_TOKEN\",\"scope\":\" email\",\"uid\":22022,\"info\":{\"name\":\"Totto\",\"email\":\"totto@totto.org\"}}";
        JSONAssert.assertEquals(expected, accessToken, false);
    }

    @Test
    public void testBuildCode() throws Exception {
    }

}