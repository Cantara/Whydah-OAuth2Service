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
import net.whydah.commands.config.ConstantValue;

@Path(OAuth2DiscoveryResource.OAUTH2DISCOVERY_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class OAuth2DiscoveryResource {
	
	 public static final String OAUTH2DISCOVERY_PATH = "/.well-known";
	 private static final ObjectMapper mapper = new ObjectMapper();
	
	 @GET
	 @Path("/openid-configuration")
	 public Response getConfig(@Context HttpServletRequest request) throws MalformedURLException {

		 JsonObjectBuilder jb = Json.createObjectBuilder()
				 .add("issuer", ConstantValue.MYURI)
				 .add("authorization_endpoint", ConstantValue.MYURI + "/authorize")
				 .add("token_endpoint", ConstantValue.MYURI + "/token")
				 .add("userinfo_endpoint", ConstantValue.MYURI + "/userinfo")
				 .add("jwks_uri", ConstantValue.MYURI + "/.well-known/jwks.json")
				 .add("scopes_supported", Json.createArrayBuilder(Arrays.asList(new String[] {
						 "READ",
						    "WRITE",
						    "DELETE",
						    "openid",
						    "scope",
						    "profile",
						    "email",
						    "address",
						    "phone"
						 })))
				 .add("response_types_supported", Json.createArrayBuilder(Arrays.asList(new String[] {
						 	"code"
						 })))
				 .add("grant_types_supported", Json.createArrayBuilder(Arrays.asList(new String[] {
						 	"authorization_code",
						    "refresh_token"
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
