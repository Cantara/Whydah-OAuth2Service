package net.whydah.service.oauth2proxyserver;

import edu.emory.mathcs.backport.java.util.Arrays;
import io.jsonwebtoken.Claims;
import net.whydah.service.authorizations.UserAuthorizationService;
import net.whydah.sso.user.types.UserToken;
import net.whydah.util.AccessTokenMapper;
import net.whydah.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;

@Path(OAuth2UserResource.OAUTH2USERINFO_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class OAuth2UserResource {
	public static final String OAUTH2USERINFO_PATH = "/userinfo";

	private final UserAuthorizationService authorizationService;

	@Autowired
	public OAuth2UserResource(UserAuthorizationService authorizationService) {
		this.authorizationService = authorizationService;
	}
	
	private String parseJwt(HttpServletRequest request) {
		String headerAuth = request.getHeader("Authorization");

		if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
			return headerAuth.substring(7, headerAuth.length());
		}

		return null;
	}
	
	@GET
	public Response getUserInfo(@Context HttpServletRequest request) throws Exception {

		String jwt = parseJwt(request);
		if (jwt != null && JwtUtils.validateJwtToken(jwt, RSAKeyFactory.getKey().getPublic())) {

			Claims claims = JwtUtils.getClaims(jwt, RSAKeyFactory.getKey().getPublic());
			UserToken userToken = authorizationService.findUserTokenFromUserTokenId(claims.get("usertoken_id", String.class));
			if (userToken == null) {
				return Response.status(Response.Status.UNAUTHORIZED).build();
			}
			JsonObjectBuilder tokenBuilder = Json.createObjectBuilder()
					.add("sub", claims.getSubject())
					.add("first_name", userToken.getFirstName())
					.add("last_name", userToken.getLastName());

			String scope = claims.get("scope", String.class);
			tokenBuilder = AccessTokenMapper.buildUserInfoJson(tokenBuilder, userToken, claims.get("app_id", String.class), Arrays.asList(scope.split(" ")));
			return Response.ok(tokenBuilder.build().toString()).build();
		} else {
			return Response.status(Response.Status.UNAUTHORIZED).build();
		}
	}
}
