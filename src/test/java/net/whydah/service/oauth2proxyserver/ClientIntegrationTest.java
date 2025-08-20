package net.whydah.service.oauth2proxyserver;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

import org.apache.http.client.utils.URIBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import net.whydah.commands.config.ConstantValues;
import net.whydah.sso.application.helpers.ApplicationXpathHelper;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.commands.appauth.CommandLogonApplication;
import net.whydah.sso.commands.userauth.CommandLogonUserByUserCredential;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserCredential;
import net.whydah.sso.user.types.UserToken;
import net.whydah.util.ClientIDUtil;

//TODO: CANNOT LOGON https://whydahdev.cantara.no/tokenservice/
@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClientIntegrationTest {

    public static final Logger log = LoggerFactory.getLogger(ClientIntegrationTest.class);

    static String OAUTH2_SERVCIE = ConstantValues.MYURI;
    //static String OAUTH2_SERVCIE = "http://localhost:9898/oauth2";//We can use this to test our local OAuth2Service "http://localhost:9898/oauth2";
    static String TOKEN_SERVICE = ConstantValues.STS_URI;

    static String TEMPORARY_APPLICATION_ID = ConstantValues.TEST_APPID;
    static String TEMPORARY_APPLICATION_NAME = ConstantValues.TEST_APPNAME;
    static String TEMPORARY_APPLICATION_SECRET = ConstantValues.TEST_APPSECRET;
    static String TEST_USERNAME = ConstantValues.TEST_USERNAME;
    static String TEST_USERPASSWORD = ConstantValues.TEST_PASSWORD;

    static String clientId = ClientIDUtil.getClientID(TEMPORARY_APPLICATION_ID);
    static String code = "";
    static String access_token = "";
    static String refresh_token = "";
    static String id_token = "";

    protected HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    static UserToken userToken = null;

    @BeforeClass 
    public static void setup() {
        String myAppTokenXml = new CommandLogonApplication(URI.create(TOKEN_SERVICE), 
                new ApplicationCredential(TEMPORARY_APPLICATION_ID, TEMPORARY_APPLICATION_NAME, TEMPORARY_APPLICATION_SECRET))
                .execute();
        assertTrue(myAppTokenXml != null);
        String myApplicationTokenID = ApplicationXpathHelper.getAppTokenIdFromAppTokenXml(myAppTokenXml);
        assertTrue(myApplicationTokenID != null && myApplicationTokenID.length() > 5);
        String userticket = UUID.randomUUID().toString();
        String ut = new CommandLogonUserByUserCredential(URI.create(TOKEN_SERVICE), myApplicationTokenID, 
                myAppTokenXml, new UserCredential(TEST_USERNAME, TEST_USERPASSWORD), userticket).execute();
        try {
            userToken = UserTokenMapper.fromUserTokenXml(ut);
            assertTrue(userToken != null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test01_askUserAuthorizationWithoutCookie_returns_301ToSSO() 
            throws JsonParseException, JsonMappingException, IOException, URISyntaxException, InterruptedException {

        URIBuilder uri = new URIBuilder(OAUTH2_SERVCIE + "/authorize");
        uri.addParameter("response_type", "code");
        uri.addParameter("client_id", clientId);
        uri.addParameter("redirect_uri", "http://localhost:3000");
        uri.addParameter("scope", "openid email phone");
        uri.addParameter("state", "1234zyx");
        uri.addParameter("nonce_01_", "nonce" + UUID.randomUUID());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri.build())
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // returns 200 from the server b/c SSO and OAuth2 have the same domain whydahdev.cantara.no?
        // returns 301 from localhost
        assertTrue(response.statusCode() == 301 || response.statusCode() == 303 || response.statusCode() == 200); // redirect to SSO
    }

    @Test
    public void test02_askUserAuthorizationWith_returns_AuthorizationForm() 
            throws JsonParseException, JsonMappingException, IOException, URISyntaxException, InterruptedException {

        String userTokenId = userToken.getUserTokenId();

        URIBuilder uri = new URIBuilder(OAUTH2_SERVCIE + "/authorize");
        uri.addParameter("response_type", "code");
        uri.addParameter("client_id", clientId);
        uri.addParameter("redirect_uri", "http://localhost:3000");
        uri.addParameter("scope", "openid email phone");
        uri.addParameter("state", "1234zyx");
        uri.addParameter("nonce_02_", "nonce" + UUID.randomUUID());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri.build())
                .header("Accept", "application/json")
                .header("Cookie", "whydahusertoken_sso=" + userTokenId)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        // returns 303 from the server running the same domain whydahdev.cantara.no
        // returns 200 from localhost
        assertTrue(response.statusCode() == 200 || response.statusCode() == 303);
    }

    @Test
    public void test03_userRejected_returns_toRedirectURIWithoutACode() 
            throws URISyntaxException, IOException, InterruptedException {

        // Form data for user rejection
        String formData = buildFormData(
                "response_mode", "query",
                "response_type", "code",
                "client_id", clientId,
                "redirect_uri", "http://localhost:3000",
                "scope", "openid email phone",
                "state", "1234zyx",
                "nonce", "nonce_03_" + UUID.randomUUID(),
                "usertoken_id", userToken.getUserTokenId(),
                "accepted", "no"
        );

        URIBuilder uri = new URIBuilder(OAUTH2_SERVCIE + "/authorize/acceptance");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri.build())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        String location = response.headers().firstValue("Location").orElse("");
        assertFalse(location.contains("code="));
        assertTrue(location.contains("state=1234zyx"));
        assertTrue(response.statusCode() == 302);
    }

    @Test
    public void test04_userAccepted_returns_toRedirectURIWithACode() 
            throws JsonParseException, JsonMappingException, IOException, URISyntaxException, InterruptedException {

        // Form data for user acceptance
        String formData = buildFormData(
                "response_type", "code",
                "client_id", clientId,
                "redirect_uri", "http://localhost:3000",
                "scope", "openid email phone",
                "state", "1234zyx",
                "nonce", "nonce_04_" + UUID.randomUUID(),
                "usertoken_id", userToken.getUserTokenId(),
                "accepted", "yes"
        );

        URIBuilder uri = new URIBuilder(OAUTH2_SERVCIE + "/authorize/acceptance");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri.build())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        String location = response.headers().firstValue("Location").orElse("");
        assertTrue(location.contains("http://localhost:3000?code="));
        assertTrue(location.contains("state=1234zyx"));
        assertTrue(response.statusCode() == 302);

        // extract code
        code = getQueryMap(URI.create(location).getQuery()).get("code");
    }

    @Test
    public void test05_tokenRequest_returns_accesstoken() 
            throws URISyntaxException, JSONException, IOException, InterruptedException {
        
        if (code == null || code.isEmpty()) {
            log.warn("Cannot further testing due to insufficient sample data");
            return;
        }

        String credentials = clientId + ":optional_secret_key";
        String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes());

        String formData = buildFormData(
                "grant_type", "authorization_code",
                "code", code,
                "state", "1234zyx",
                "nonce", "nonce_05_" + UUID.randomUUID()
        );

        URIBuilder uri = new URIBuilder(OAUTH2_SERVCIE + "/token");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri.build())
                .header("Authorization", "Basic " + basicAuth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        log.info("Response {}", response.body());
        JSONObject d = new JSONObject(response.body());
        assertTrue(d.has("access_token"));
        assertTrue(d.has("token_type"));
        assertTrue(d.has("expires_in"));
        assertTrue(d.has("refresh_token"));
        assertTrue(d.has("nonce"));
        assertTrue(d.has("id_token"));

        access_token = d.getString("access_token");
        refresh_token = d.getString("refresh_token");
        id_token = d.getString("id_token");
        System.out.print("Access token:" + access_token);
    }

    @Test
    public void test05_xGET_refreshtokenRequest_returns_accesstoken() 
            throws URISyntaxException, JSONException, IOException, InterruptedException {
        
        // First get a new code
        String formData = buildFormData(
                "response_type", "code",
                "client_id", clientId,
                "redirect_uri", "http://localhost:3000",
                "scope", "openid email phone",
                "state", "1234zyx",
                "nonce", "nonce_05_GET_1__" + UUID.randomUUID(),
                "usertoken_id", userToken.getUserTokenId(),
                "accepted", "yes"
        );

        URIBuilder uri = new URIBuilder(OAUTH2_SERVCIE + "/authorize/acceptance");

        HttpRequest request1 = HttpRequest.newBuilder()
                .uri(uri.build())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        HttpResponse<String> response1 = httpClient.send(request1, HttpResponse.BodyHandlers.ofString());

        String location = response1.headers().firstValue("Location").orElse("");
        assertTrue(location.contains("http://localhost:3000?code="));
        assertTrue(location.contains("state=1234zyx"));
        assertTrue(response1.statusCode() == 302);

        // extract code
        code = getQueryMap(URI.create(location).getQuery()).get("code");

        if (code == null || code.isEmpty()) {
            log.warn("Cannot further testing due to insufficient sample data");
            return;
        }

        String credentials = clientId + ":optional_secret_key";
        String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes());

        URIBuilder uri2 = new URIBuilder(OAUTH2_SERVCIE + "/token");
        uri2.addParameter("grant_type", "authorization_code");
        uri2.addParameter("code", code);
        uri2.addParameter("redirect_uri", "http://localhost:3000");
        uri2.addParameter("scope", "openid email phone");
        uri2.addParameter("state", "1234zyx");
        uri2.addParameter("nonce", "nonce_05_GET_2_" + UUID.randomUUID());

        HttpRequest request2 = HttpRequest.newBuilder()
                .uri(uri2.build())
                .header("Authorization", "Basic " + basicAuth)
                .GET()
                .build();

        HttpResponse<String> response2 = httpClient.send(request2, HttpResponse.BodyHandlers.ofString());
        
        // returns 303 from the server running the same domain whydahdev.cantara.no
        // returns 200 from localhost
        assertTrue(response2.statusCode() == 200 || response2.statusCode() == 303);

        log.info("Response {}", response2.body());
        JSONObject d = new JSONObject(response2.body());
        assertTrue(d.has("access_token"));
        assertTrue(d.has("token_type"));
        assertTrue(d.has("expires_in"));
        assertTrue(d.has("refresh_token"));
        assertTrue(d.has("nonce"));
        assertTrue(d.has("id_token"));

        access_token = d.getString("access_token");
        refresh_token = d.getString("refresh_token");
        id_token = d.getString("id_token");
    }

    @Test
    public void test06_refreshtokenRequest_returns_accesstoken() 
            throws URISyntaxException, JSONException, IOException, InterruptedException {
        if (refresh_token == null || refresh_token.isEmpty()) {
            log.warn("Cannot further testing due to insufficient sample data");
            return;
        }

        String credentials = clientId + ":optional_secret_key";
        String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes());

        String formData = buildFormData(
                "grant_type", "refresh_token",
                "nonce", "nonce_06_" + UUID.randomUUID(),
                "refresh_token", refresh_token
        );

        URIBuilder uri = new URIBuilder(OAUTH2_SERVCIE + "/token");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri.build())
                .header("Authorization", "Basic " + basicAuth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        log.info("Response {}", response.body());
        JSONObject d = new JSONObject(response.body());
        assertTrue(d.has("access_token"));
        assertTrue(d.has("token_type"));
        assertTrue(d.has("expires_in"));
        assertTrue(d.has("refresh_token"));
        assertTrue(d.has("id_token"));

        access_token = d.getString("access_token");
        refresh_token = d.getString("refresh_token");
        id_token = d.getString("id_token");
    }

    @Test
    public void test07_getUserInfo_returns_accesstoken() 
            throws URISyntaxException, JSONException, IOException, InterruptedException {
        if (access_token == null || access_token.isEmpty()) {
            log.warn("Cannot further testing due to insufficient sample data");
            return;
        }

        URIBuilder uri = new URIBuilder(OAUTH2_SERVCIE + "/userinfo");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri.build())
                .header("Authorization", "Bearer " + access_token)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        log.info("Response {}", response.body());
        JSONObject d = new JSONObject(response.body());
        assertTrue(d.has("sub"));
        assertTrue(d.has("first_name"));
        assertTrue(d.has("last_name"));
        assertTrue(d.has("email"));
        assertTrue(d.has("phone"));
    }

    // Helper method to build form data
    private String buildFormData(String... params) {
        StringJoiner formData = new StringJoiner("&");
        for (int i = 0; i < params.length; i += 2) {
            String key = URLEncoder.encode(params[i], StandardCharsets.UTF_8);
            String value = URLEncoder.encode(params[i + 1], StandardCharsets.UTF_8);
            formData.add(key + "=" + value);
        }
        return formData.toString();
    }

    public static Map<String, String> getQueryMap(String query) {
        if (query == null || query.isEmpty()) {
            return new HashMap<>();
        }
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<String, String>();

        for (String param : params) {
            String[] keyValue = param.split("=", 2);
            if (keyValue.length == 2) {
                map.put(keyValue[0], keyValue[1]);
            }
        }
        return map;
    }
}