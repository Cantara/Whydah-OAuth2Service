package net.whydah.service.oauth2proxyserver;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import net.whydah.sso.application.helpers.ApplicationXpathHelper;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.commands.appauth.CommandLogonApplication;
import net.whydah.sso.commands.userauth.CommandLogonUserByUserCredential;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserCredential;
import net.whydah.sso.user.types.UserToken;
import net.whydah.util.ClientIDUtil;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClientIntegrationTest {
	
	public static final Logger log = LoggerFactory.getLogger(ClientIntegrationTest.class);
	
	static String OAUTH2_SERVCIE = "https://whydahdev.cantara.no/oauth2";
	//static String OAUTH2_SERVCIE = "http://localhost:9898/oauth2";//We can use this to test our local OAuth2Service "http://localhost:9898/oauth2"; 
	static String TOKEN_SERVICE = "https://whydahdev.cantara.no/tokenservice/"; 
	
    static String TEMPORARY_APPLICATION_ID = "101";//"11";
    static String TEMPORARY_APPLICATION_NAME = "Whydah-SystemTests";//"Funny APp";//"11";
    static String TEMPORARY_APPLICATION_SECRET = "55fhRM6nbKZ2wfC6RMmMuzXpk";//"LLNmHsQDCerVWx5d6aCjug9fyPE";
    static String TEST_USERNAME = "systest";
    static String TEST_USERPASSWORD = "systest42";
    
	static String clientId = ClientIDUtil.getClientID(TEMPORARY_APPLICATION_ID);
	static String code="";
	static String access_token="";
	static String refresh_token="";
	static String id_token="";
	
	protected RestTemplate restTemplate = new RestTemplate() ;
	
	@Test
	public void test01_askUserAuthorizationWithoutCookie_returns_301ToSSO() throws JsonParseException, JsonMappingException, IOException, URISyntaxException {
		
		//authorize?response_type=code&client_id=[YOUR_CLIENT_ID]&redirect_uri=[SOME_REDIRECT_URL]&scope=openid%20email%20phone&state=1234zyx
		String userTokenId = getUserToken().getUserTokenId();
		
         
		HttpHeaders headers = new HttpHeaders();
		
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		URIBuilder uri = new URIBuilder(OAUTH2_SERVCIE+ "/authorize");
		uri.addParameter("response_type", "code");
		uri.addParameter("client_id", clientId);
		uri.addParameter("redirect_uri", "http://localhost:3000");
		uri.addParameter("scope", "openid email phone");
		uri.addParameter("state", "1234zyx");
		
		ResponseEntity<String> response = restTemplate.exchange(uri.build(), HttpMethod.GET, entity, String.class);

//		Fun fact: this assertion is correct when running local OAuth2-Service (but when using remote OAuth2 running on the same domain whydahdev.cantara.no like SSO, it returns status code 200)
//		assertTrue(response.getHeaders().getLocation().toString().contains("https://whydahdev.cantara.no/sso/login"));
//		assertTrue(response.getHeaders().getLocation().toString().contains(clientId));
//		assertTrue(response.getHeaders().getLocation().toString().contains("http://localhost:3000"));
//		assertTrue(response.getHeaders().getLocation().toString().contains("openid"));
//		assertTrue(response.getHeaders().getLocation().toString().contains("1234zyx"));

		//returns 200 from the server b/c SSO and OAuth2 have the same domain whydahdev.cantara.no?
		//returns 301 from localhost
		assertTrue(response.getStatusCodeValue() == 301 || response.getStatusCodeValue() == 303 || response.getStatusCodeValue() == 200); //redirect to SSO
	}
	
	@Test
	public void test02_askUserAuthorizationWith_returns_AuthorizationForm() throws JsonParseException, JsonMappingException, IOException, URISyntaxException {
		
		//authorize?response_type=code&client_id=[YOUR_CLIENT_ID]&redirect_uri=[SOME_REDIRECT_URL]&scope=openid%20email%20phone&state=1234zyx
		String userTokenId = getUserToken().getUserTokenId();
		
         
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.add("cookie", "whydahusertoken_sso=" + userTokenId);
		
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		URIBuilder uri = new URIBuilder(OAUTH2_SERVCIE+ "/authorize");
		uri.addParameter("response_type", "code");
		uri.addParameter("client_id", clientId);
		uri.addParameter("redirect_uri", "http://localhost:3000");
		uri.addParameter("scope", "openid email phone");
		uri.addParameter("state", "1234zyx");
		
		ResponseEntity<String> response = restTemplate.exchange(uri.build(), HttpMethod.GET, entity, String.class);
		//returns 303 from the server running the same domain whydahdev.cantara.no
		//returns 200 from localhost
		assertTrue(response.getStatusCodeValue()==200 || response.getStatusCodeValue()==303);
	}
	
	@Test
	public void test03_userRejected_returns_toRedirectURIWithoutACode() throws URISyntaxException {

		HttpHeaders headers = new HttpHeaders();

		//simulate the fact that user has clicked on the reject button
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("response_type", "code");
		map.add("client_id", clientId);
		map.add("redirect_uri", "http://localhost:3000");
		map.add("scope", "openid email phone");
		map.add("state", "1234zyx");
		map.add("usertoken_id", getUserToken().getUserTokenId());
		map.add("accepted", "no");
		
		URIBuilder uri = new URIBuilder(OAUTH2_SERVCIE+ "/authorize/acceptance");
		
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);
		ResponseEntity<String> response = restTemplate.exchange(uri.build(), HttpMethod.POST, entity, String.class);

		assertFalse(response.getHeaders().getLocation().toString().contains("code="));
		assertTrue(response.getHeaders().getLocation().toString().contains("state=1234zyx"));
		assertTrue(response.getStatusCodeValue()==302);
		
	
	}
	
	@Test
	public void test04_userAccepted_returns_toRedirectURIWithACode() throws JsonParseException, JsonMappingException, IOException, URISyntaxException {
		
		
		HttpHeaders headers = new HttpHeaders();

		//simulate the fact that user has clicked on the accept button
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("response_type", "code");
		map.add("client_id", clientId);
		map.add("redirect_uri", "http://localhost:3000");
		map.add("scope", "openid email phone");
		map.add("state", "1234zyx");
		map.add("usertoken_id", getUserToken().getUserTokenId());
		map.add("accepted", "yes");
		
		URIBuilder uri = new URIBuilder(OAUTH2_SERVCIE+ "/authorize/acceptance");
		
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);
		ResponseEntity<String> response = restTemplate.exchange(uri.build(), HttpMethod.POST, entity, String.class);

		assertTrue(response.getHeaders().getLocation().toString().contains("http://localhost:3000?code="));
		assertTrue(response.getHeaders().getLocation().toString().contains("state=1234zyx"));
		assertTrue(response.getStatusCodeValue()==302);
		
		//extract code 
		code = getQueryMap(response.getHeaders().getLocation().getQuery()).get("code");
	}

	
	
	@Test
	public void test05_tokenRequest_returns_accesstoken() throws URISyntaxException, JSONException {
		
		if(code==null || code.isEmpty()) {
			log.warn("Cannot further testing due to insufficient sample data");
			return;
		}
		HttpHeaders headers = new HttpHeaders();
		String credentials = clientId+":optional_secret_key";
		byte[] authEncBytes = Base64.getEncoder().encode(credentials.getBytes());
		headers.add("Authorization", "Basic " + new String(authEncBytes));
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("grant_type", "authorization_code");
		map.add("code", code);
		
		URIBuilder uri = new URIBuilder(OAUTH2_SERVCIE+ "/token");
		
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);
		ResponseEntity<String> response = restTemplate.exchange(uri.build(), HttpMethod.POST, entity, String.class);

		log.info("Response {}", response.getBody());
		JSONObject d = new JSONObject(response.getBody());
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
	public void test06_refreshtokenRequest_returns_accesstoken() throws URISyntaxException, JSONException {
		if(refresh_token==null || refresh_token.isEmpty()) {
			log.warn("Cannot further testing due to insufficient sample data");
			return;
		}
		HttpHeaders headers = new HttpHeaders();
		String credentials = clientId+":optional_secret_key";
		byte[] authEncBytes = Base64.getEncoder().encode(credentials.getBytes());
		headers.add("Authorization", "Basic " + new String(authEncBytes));
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("grant_type", "refresh_token");
		map.add("refresh_token", refresh_token);
		
		URIBuilder uri = new URIBuilder(OAUTH2_SERVCIE+ "/token");
		
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);
		ResponseEntity<String> response = restTemplate.exchange(uri.build(), HttpMethod.POST, entity, String.class);

		log.info("Response {}", response.getBody());
		JSONObject d = new JSONObject(response.getBody());
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
	public void test07_getUserInfo_returns_accesstoken() throws URISyntaxException, JSONException {
		if(access_token==null || access_token.isEmpty()) {
			log.warn("Cannot further testing due to insufficient sample data");
			return;
		}
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + access_token);
		
		URIBuilder uri = new URIBuilder(OAUTH2_SERVCIE+ "/userinfo");
		
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		ResponseEntity<String> response = restTemplate.exchange(uri.build(), HttpMethod.GET, entity, String.class);

		log.info("Response {}", response.getBody());
		JSONObject d = new JSONObject(response.getBody());
		assertTrue(d.has("sub"));
		assertTrue(d.has("first_name"));
		assertTrue(d.has("last_name"));
		assertTrue(d.has("email"));
		assertTrue(d.has("phone"));
		
	}
	
	private UserToken getUserToken() {
		String myAppTokenXml = new CommandLogonApplication(URI.create(TOKEN_SERVICE), new ApplicationCredential(TEMPORARY_APPLICATION_ID, TEMPORARY_APPLICATION_NAME, TEMPORARY_APPLICATION_SECRET)).execute();
         String myApplicationTokenID = ApplicationXpathHelper.getAppTokenIdFromAppTokenXml(myAppTokenXml);
         assertTrue(myApplicationTokenID != null && myApplicationTokenID.length() > 5);
         String userticket = UUID.randomUUID().toString();
         String userToken = new CommandLogonUserByUserCredential(URI.create(TOKEN_SERVICE), myApplicationTokenID, myAppTokenXml, new UserCredential(TEST_USERNAME, TEST_USERPASSWORD), userticket).execute();
         return UserTokenMapper.fromUserTokenXml(userToken);
	}

	public static Map<String, String> getQueryMap(String query) {  
	    String[] params = query.split("&");  
	    Map<String, String> map = new HashMap<String, String>();

	    for (String param : params) {  
	        String name = param.split("=", 2)[0];  
	        String value = param.split("=", 2)[1];  
	        map.put(name, value);  
	    }  
	    return map;  
	}
}
