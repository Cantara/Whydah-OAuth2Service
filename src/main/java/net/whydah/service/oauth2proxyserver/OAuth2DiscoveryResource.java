package net.whydah.service.oauth2proxyserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.whydah.commands.config.ConstantValues;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.net.MalformedURLException;
import java.util.Arrays;

@Path(OAuth2DiscoveryResource.OAUTH2DISCOVERY_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class OAuth2DiscoveryResource {
	
	 public static final String OAUTH2DISCOVERY_PATH = "/.well-known";
	 private static final ObjectMapper mapper = new ObjectMapper();
	
	 @GET
	 @Path("/openid-configuration")
	 public Response getConfig(@Context HttpServletRequest request) throws MalformedURLException {

		 JsonObjectBuilder jb = Json.createObjectBuilder()
				 .add("issuer", ConstantValues.MYURI)
				 .add("authorization_endpoint", ConstantValues.MYURI + "/authorize")
				 .add("token_endpoint", ConstantValues.MYURI + "/token")
				 .add("userinfo_endpoint", ConstantValues.MYURI + "/userinfo")
				 .add("jwks_uri", ConstantValues.MYURI + "/.well-known/jwks.json")
				 .add("end_session_endpoint", ConstantValues.MYURI + "/logout")
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
				 /*
				 .add("claims_parameter_supported", Json.createArrayBuilder(Arrays.asList(
						  "uid",
						  "email",
						  "phone_number",
						  "family_name",
						  "given_name",
						  "customer_ref",
						  "security_level",
						  "last_seen",
						  "roles",
						  
						  //old incompatible version
						  "first_name",
						  "last_name",
						  "phone"
						  
						 )))
				 */
				 .add("claims_parameter_supported", false)
				 .add("request_parameter_supported", false)
				 .add("request_uri_parameter_supported", false);
		 
		 return Response.ok(jb.build().toString()).build(); 
	 }

	 @GET
	 @Path("/jwks.json")
	 public Response getJWKS(@Context HttpServletRequest request) throws Exception {

		 /*
		 RSAPublicKey rsa = (RSAPublicKey) RSAKeyFactory.getKey().getPublic();
		 RSAKey key = new RSAKey.Builder(rsa)
				 .keyID(RSAKeyFactory.getKid())
				 .keyUse(new KeyUse("sig"))
				 .algorithm(new Algorithm("RS256"))
				 .build();
		 
		 
		
		 return Response.ok( mapper.writeValueAsString(new JWKSet(key).toJSONObject())).build(); 
		 */
		 return Response.ok( mapper.writeValueAsString(new JWKSet(RSAKeyFactory.getRsaKeys()).toJSONObject())).build(); 
		
	     
	 }
	 
}
