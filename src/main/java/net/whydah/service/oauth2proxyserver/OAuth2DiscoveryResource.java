package net.whydah.service.oauth2proxyserver;

import java.net.MalformedURLException;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;

import edu.emory.mathcs.backport.java.util.Arrays;
import net.whydah.commands.config.ConfiguredValue;

@Path(OAuth2DiscoveryResource.OAUTH2DISCOVERY_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class OAuth2DiscoveryResource {
	
	 public static final String OAUTH2DISCOVERY_PATH = "/.well-known";
	 private static final ObjectMapper mapper = new ObjectMapper();
	
	 @GET
	 @Path("/openid-configuration")
	 public Response getConfig(@Context HttpServletRequest request) throws MalformedURLException {

		 JsonObjectBuilder jb = Json.createObjectBuilder()
				 .add("issuer", ConfiguredValue.MYURI)
				 .add("authorization_endpoint", ConfiguredValue.MYURI + "/authorize")
				 .add("token_endpoint", ConfiguredValue.MYURI + "/token")
				 .add("userinfo_endpoint", ConfiguredValue.MYURI + "/userinfo")
				 .add("jwks_uri", ConfiguredValue.MYURI + "/.well-known/jwks.json")
				 .add("end_session_endpoint", ConfiguredValue.MYURI + "/logout")
				 .add("scopes_supported", Json.createArrayBuilder(Arrays.asList(new String[] {
						    "openid",
						    "profile",
						    "email",
						    "address",
						    "phone",
						    "offline_access"
						 })))
				 .add("response_types_supported", Json.createArrayBuilder(Arrays.asList(new String[] {
						 	"code",
						 	"token",
							"id_token",
							"id_token token",
							"code id_token",
							"code token",
							"code id_token token",
							"none"
						 })))
				 .add("grant_types_supported", Json.createArrayBuilder(Arrays.asList(new String[] {
						 	"authorization_code",
						    "refresh_token",
						    "password",
						    "client_credentials"
						 })))
				 .add("subject_types_supported", Json.createArrayBuilder(Arrays.asList(new String[] {
						 	"public"
						 })))
				 .add("id_token_signing_alg_values_supported", Json.createArrayBuilder(Arrays.asList(new String[] {
						 	"RS256"
						 })))		 
				 .add("token_endpoint_auth_methods_supported", Json.createArrayBuilder(Arrays.asList(new String[] {
						 	"client_secret_basic",
						 	"client_secret_post"
						 })))
				 .add("token_endpoint_auth_signing_alg_values_supported", Json.createArrayBuilder(Arrays.asList(new String[] {
						 	"RS256"
						 })))	
				 .add("claims_parameter_supported", false)
				 .add("request_parameter_supported", false)
				 .add("request_uri_parameter_supported", false);
		 
		 return Response.ok(jb.build().toString()).build(); 
	 }
	 
	 @GET
	 @Path("/jwks.json")
	 public Response getJWKS(@Context HttpServletRequest request) throws Exception {

		 RSAPublicKey rsa = (RSAPublicKey) RSAKeyFactory.getKey().getPublic();
		 
//		 Map<String, Object> values = new HashMap<>();
//
//		 values.put("kty", rsa.getAlgorithm()); // getAlgorithm() returns kty not algorithm
//		 values.put("kid", RSAKeyFactory.getKid());
//		 values.put("n", Base64.getUrlEncoder().encodeToString(rsa.getModulus().toByteArray()));
//		 values.put("e", Base64.getUrlEncoder().encodeToString(rsa.getPublicExponent().toByteArray()));
//		 values.put("alg", "RS256");
//		 values.put("use", "sig");

		 RSAKey key = new RSAKey.Builder(rsa)
				 .keyID(RSAKeyFactory.getKid())
				 .keyUse(new KeyUse("sig"))
				 .algorithm(new Algorithm("RS256"))
				 .build();
		
		 return Response.ok( mapper.writeValueAsString(new JWKSet(key).toJSONObject())).build(); 
	     
	 }
	 
}
